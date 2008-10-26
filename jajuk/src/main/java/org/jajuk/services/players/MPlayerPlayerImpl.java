/*
 *  Jajuk
 *  Copyright (C) 2003 The Jajuk Team
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *  $Revision:3266 $
 */
package org.jajuk.services.players;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.StringTokenizer;

import org.jajuk.base.Track;
import org.jajuk.services.webradio.WebRadio;
import org.jajuk.util.Conf;
import org.jajuk.util.Const;
import org.jajuk.util.error.JajukException;
import org.jajuk.util.log.Log;

/**
 * Jajuk player implementation based on Mplayer
 */
public class MPlayerPlayerImpl extends AbstractMPlayerImpl {

  /**
   * The time we wait for mplayer to start up. It can take some time on slow or
   * heavily loaded machines...
   */
  private static final int MPLAYER_START_TIMEOUT = 30;

  /** Time elapsed in ms */
  private long lTime = 0;

  /** Length to be played in secs */
  private long length;

  /** Starting position */
  private float fPosition;

  /** Current track estimated duration in ms */
  private long lDuration;

  /** Volume when starting fade */
  private float fadingVolume;

  /** Cross fade duration in ms */
  int iFadeDuration = 0;

  /**
   * Progress step in ms, do not set less than 300 or 400 to avoid using too
   * much CPU
   */
  private static final int PROGRESS_STEP = 500;

  /** current file */
  private org.jajuk.base.File fCurrent;

  /** pause flag * */
  private volatile boolean bPaused = false;

  /** Inc rating flag */
  // private boolean bHasBeenRated = false;
  /**
   * Position and elapsed time getter
   */
  private class PositionThread extends Thread {
    public PositionThread(String name) {
      super(name);
    }

    @Override
    public void run() {
      int comp = 1;
      Track current = fCurrent.getTrack();
      while (!bStop) { // stop this thread when exiting
        try {
          if (!bPaused && !bStop) {
            // a get_percent_pos resumes (mplayer issue)
            sendCommand("get_time_pos");
            // every 2 time units, increase actual play time. We wait this
            // delay for perfs and for precision
            if (comp % 2 == 0) {
              // Increase actual play time
              // End of file: increase actual play time to the track
              // Perf note : this full action takes less much than 1 ms
              long trackPlaytime = current.getLongValue(Const.XML_TRACK_TOTAL_PLAYTIME);
              long newValue = PROGRESS_STEP / 100 + trackPlaytime;
              current.setProperty(Const.XML_TRACK_TOTAL_PLAYTIME, +newValue);
            }
            comp++;
          }
          Thread.sleep(PROGRESS_STEP);
        } catch (Exception e) {
          Log.error(e);
        }
      }
    }
  }

  /**
   * Reader : read information from mplayer like position
   */
  private class ReaderThread extends Thread {
    public ReaderThread(String name) {
      super(name);
    }

    @Override
    public void run() {
      try {
        BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String line = null;
        for (; true;) {
          line = in.readLine();
          if (line == null) {
            break;
          }

          // produces lots of output: Log.debug("Output from MPlayer: " + line);

          if (line.matches(".*ANS_TIME_POSITION.*")) {
            // Stream no more opening
            bOpening = false;
            StringTokenizer st = new StringTokenizer(line, "=");
            st.nextToken();
            lTime = (int) (Float.parseFloat(st.nextToken()) * 1000);
            // Store current position for use at next startup
            Conf
                .setProperty(Const.CONF_STARTUP_LAST_POSITION, Float.toString(getCurrentPosition()));
            // Cross-Fade test
            if (!bFading && iFadeDuration > 0 && lDuration > 0
            // can be null before getting length
                && lTime > (lDuration - iFadeDuration)
                // do not fade if the track is very short
                && (lTime > 3 * iFadeDuration)) {
              bFading = true;
              fadingVolume = fVolume;
              // force a finished (that doesn't stop but only
              // make a FIFO request to switch track)
              FIFO.finished();
            }
            // If fading, decrease sound progressively
            if (bFading) {
              // computes the volume we have to sub to reach zero
              // at last
              // progress()
              float fVolumeStep = fadingVolume
              // we double the refresh period to make sure to
                  // reach 0 at the end of iterations because
                  // we don't
                  // as many mplayer response as queries,
                  // tested on 10 & 20 sec of fading
                  * ((float) PROGRESS_STEP / iFadeDuration);
              float fNewVolume = fVolume - fVolumeStep;
              // decrease volume by n% of initial volume
              if (fNewVolume < 0) {
                fNewVolume = 0;
              }
              try {
                setVolume(fNewVolume);
              } catch (Exception e) {
                Log.error(e);
              }
            }
            // test end of length for intro mode
            if (length != TO_THE_END && lDuration > 0
            // can be null before getting length
                && (lTime - (fPosition * lDuration)) > length) {
              // length=-1 means there is no max length
              bFading = false;
              FIFO.finished();
            }
          } else if (line.matches("ANS_LENGTH.*")) {
            StringTokenizer st = new StringTokenizer(line, "=");
            st.nextToken();
            lDuration = (long) (Float.parseFloat(st.nextToken()) * 1000);
          }
          // End of file
          else if (line.matches(".*\\x2e\\x2e\\x2e.*\\(.*\\).*")) {
            bEOF = true;
            // Launch next track
            try {
              // Update track rate
              fCurrent.getTrack().updateRate();

              // inc rate by 1 if file is fully
              // played
              // fCurrent.getTrack().setRate(fCurrent.getTrack().getRate() + 1);
              // Alert rating manager that something changed
              // RatingManager.setRateHasChanged(true);
              // If using crossfade, ignore end of file
              if (!bFading
              // Do not launch next track if not opening: it means
                  // that the file is in error (EOF comes
                  // before any
                  // play) and the finished() is processed by
                  // Player
                  // on exception processing
                  && !bOpening) {
                // Benefit from end of file to perform a full gc
                System.gc();
                FIFO.finished();
              } else {
                // If fading, next track has already been
                // launched
                bFading = false;
              }
            } catch (Exception e) {
              Log.error(e);
            }
            break;
          }
        }
        // can reach this point at the end of file
        in.close();
      } catch (IOException e) {
        Log.error(e);
      }
    }
  }

  @Override
  public void stop() throws Exception {
    // Update track rate
    fCurrent.getTrack().updateRate();
    // Call generic stop
    super.stop();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.players.IPlayerImpl#play(org.jajuk.base.File, float, long,
   *      float)
   */
  @Override
  public void play(org.jajuk.base.File file, float fPosition, long length, float fVolume)
      throws Exception {
    this.lTime = 0;
    this.fVolume = fVolume;
    this.length = length;
    this.fPosition = fPosition;
    this.bFading = false;
    this.fCurrent = file;
    this.bOpening = true;
    // this.bHasBeenRated = false;
    this.bEOF = false;
    this.iFadeDuration = 1000 * Conf.getInt(Const.CONF_FADE_DURATION);
    ProcessBuilder pb = new ProcessBuilder(buildCommand(file.getAbsolutePath()));
    Log.debug("Using this Mplayer command: {{" + pb.command() + "}}");
    // Set all environment variables format: var1=xxx var2=yyy
    try {
      Map<String, String> env = pb.environment();
      StringTokenizer st = new StringTokenizer(Conf.getString(Const.CONF_ENV_VARIABLES), " ");
      while (st.hasMoreTokens()) {
        StringTokenizer st2 = new StringTokenizer(st.nextToken(), "=");
        env.put(st2.nextToken(), st2.nextToken());
      }
    } catch (Exception e) {
      Log.error(e);
    }
    // Start mplayer
    proc = pb.start();
    // start mplayer replies reader thread
    new ReaderThread("MPlayer reader thread").start();
    // start writer to mplayer thread
    new PositionThread("MPlayer writer thread").start();
    // if opening, wait
    int i = 0;
    // Try to open the file during 30 secs
    while (bOpening && !bEOF && i < MPLAYER_START_TIMEOUT * 100) {
      try {
        Thread.sleep(10);
        i++;
      } catch (InterruptedException e) {
        Log.error(e);
      }
    }

    // Check the file has been property opened
    if (!bOpening && !bEOF) {
      if (fPosition > 0.0f) {
        seek(fPosition);
      }
      // Get track length
      sendCommand("get_time_length");
    } else {
      // try to kill the mplayer process if still alive
      if (proc != null) {
        new Thread("MPlayer process kill thread") {
          @Override
          public void start() {
            Log.debug("OOT Mplayer process, try to kill it");
            proc.destroy();
            Log.debug("OK, the process should have been killed");
          }
        }.start();
      }
      // Notify the problem opening the file
      throw new JajukException(7, Integer.valueOf(MPLAYER_START_TIMEOUT).toString());
    }
  }

  /**
   * @return current position as a float ex: 0.2f
   */
  @Override
  public float getCurrentPosition() {
    if (lDuration == 0) {
      return 0;
    }
    return ((float) lTime) / lDuration;
  }

  /**
   * @return Returns the lTime in ms
   */
  @Override
  public long getElapsedTime() {
    return lTime;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.players.IPlayerImpl#pause()
   */
  @Override
  public void pause() throws Exception {
    bPaused = true;
    sendCommand("pause");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.players.IPlayerImpl#resume()
   */
  @Override
  public void resume() throws Exception {
    bPaused = false;
    sendCommand("pause");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.players.IPlayerImpl#seek(float) Ogg vorbis seek not yet
   *      supported
   */
  @Override
  public void seek(float posValue) {
    // if fading, ignore
    if (bFading) {
      return;
    }
    // save current position
    String command = "seek " + (int) (100 * posValue) + " 1";
    sendCommand(command);
    setVolume(fVolume); // need this because a seek reset volume
  }

  /**
   * @return player state, -1 if player is null.
   */
  @Override
  public int getState() {
    if (bFading) {
      return FADING_STATUS;
    } else {
      return -1;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.players.IPlayerImpl#getCurrentLength()
   */
  @Override
  public long getCurrentLength() {
    return lDuration;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.players.AbstractMPlayerImpl#play(org.jajuk.base.WebRadio,
   *      float)
   */
  @Override
  public void play(WebRadio radio, float volume) throws Exception {
  }

}
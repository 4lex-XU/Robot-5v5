/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/CampFire.java 2014-11-04 buixuan.
 * ******************************************************/
package algorithms;

import java.util.ArrayList;
import java.util.Random;

import robotsimulator.Brain;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.Parameters;

public class CampFire extends Brain {
  private boolean turnTask,turnRight,endMove,taskOne;
  private double endTaskDirection;
  private int endTaskCounter,id,latence;
  private static IFrontSensorResult.Types WALL=IFrontSensorResult.Types.WALL;
  private Random gen;

  public CampFire() { super(); gen = new Random(); }

  public void activate() {
    latence=-1;
    turnTask=true;
    endMove=false;
    taskOne=true;
    endTaskDirection=getHeading()+0.5*Math.PI;
    stepTurn(Parameters.Direction.RIGHT);
    sendLogMessage("Rocking and rolling.");
  }
  public void step() {
    if (getHealth()<=0) { sendLogMessage("I'm dead.");return; }
    if (endMove) { sendLogMessage("Camping point. Task one complete."); campFire(); return; }
    if (turnTask) {
      if (isHeading(endTaskDirection)) {
	turnTask=false;
        if (taskOne) endTaskCounter=700; else if (id==1) endTaskCounter=400; else endTaskCounter=250;
	move();
      } else {
        if (taskOne) stepTurn(Parameters.Direction.RIGHT);
        else stepTurn(Parameters.Direction.LEFT);
      }
      return;
    }
    if (endTaskCounter>0) {
      endTaskCounter--;
      move();
      return;
    } else {
      if (taskOne) taskOne=false; else { endMove=true;return; }
      id=0;
      ArrayList<IRadarResult> radarResults = detectRadar();
      for (IRadarResult r : radarResults)
        if (r.getObjectType()==IRadarResult.Types.TeamMainBot ||
            r.getObjectType()==IRadarResult.Types.TeamSecondaryBot) id++;
      if (id==2) id=3; else if (id==3) id=2;
      if (id==3) {
        endMove=true;
      } else {
        turnTask=true;
        endTaskDirection=getHeading()-0.5*Math.PI;
        stepTurn(Parameters.Direction.LEFT);
      }
      return;
    }
  }
  private void campFire() {
    ArrayList<IRadarResult> radarResults = detectRadar();
    int enemyFighters=0,enemyPatrols=0;
    double enemyDirection=0;
    for (IRadarResult r : radarResults) {
      if (r.getObjectType()==IRadarResult.Types.OpponentMainBot) {
        enemyFighters++;
        enemyDirection=r.getObjectDirection();
        continue;
      }
      if (r.getObjectType()==IRadarResult.Types.OpponentSecondaryBot) {
        if (enemyFighters==0) enemyDirection=r.getObjectDirection();
        enemyPatrols++;
      }
    }
    if (latence<0) {
      if (enemyFighters+enemyPatrols==0) {
        if (id==1) fire(Math.PI*(0.98+0.04*gen.nextDouble()));
        if (id==2) fire(Math.PI*(0.60+0.4*gen.nextDouble()));
        if (id==3) fire(Math.PI*(0.60+0.2*gen.nextDouble()));
        latence=21;
        return;
      }
      fire(enemyDirection);
      latence=21;
      return;
    } else latence--;

  }
  private boolean isHeading(double dir) {
    return Math.abs(Math.sin(getHeading()-dir))<Parameters.teamBSecondaryBotStepTurnAngle;
  }
}

/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/BootingBerzerk.java 2014-11-03 buixuan.
 * ******************************************************/
package algorithms;

import java.util.ArrayList;

import robotsimulator.Brain;
import characteristics.Parameters;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;

public class BootingBerzerk extends Brain {
  //---PARAMETERS---//
  private static final double HEADINGPRECISION = 0.001;

  //---VARIABLES---//
  private boolean turnTask,turnRight,moveTask,berzerk,back;
  private double endTaskDirection,lastSeenDirection;
  private int endTaskCounter,berzerkInerty;
  private boolean firstMove,berzerkTurning;

  //---CONSTRUCTORS---//
  public BootingBerzerk() { super(); }

  //---ABSTRACT-METHODS-IMPLEMENTATION---//
  public void activate() {
    turnTask=true;
    moveTask=false;
    firstMove=true;
    berzerk=false;
    berzerkInerty=0;
    berzerkTurning=false;
    back=false;
    endTaskDirection=(Math.random()-0.5)*0.5*Math.PI;
    turnRight=(endTaskDirection>0);
    endTaskDirection+=getHeading();
    lastSeenDirection=Math.random()*Math.PI*2;
    if (turnRight) stepTurn(Parameters.Direction.RIGHT);
    else stepTurn(Parameters.Direction.LEFT);
    sendLogMessage("Turning point. Waza!");
  }
  public void step() {
    /*if (Math.random()<0.01 && !berzerk) {
      fire(Math.random()*Math.PI*2);
      return;
    }*/
    ArrayList<IRadarResult> radarResults = detectRadar();
    if (berzerk) {
      if (berzerkTurning) {
        endTaskCounter--;
        if (isHeading(endTaskDirection)) {
          berzerkTurning=false;
          move();
          sendLogMessage("Moving a head. Waza!");
        } else {
          if (turnRight) stepTurn(Parameters.Direction.RIGHT);
          else stepTurn(Parameters.Direction.LEFT);
        }
        return;
      }
      /*if (endTaskCounter<0) {
        turnTask=true;
        moveTask=false;
        berzerk=false;
        endTaskDirection=(Math.random()-0.5)*2*Math.PI;
        turnRight=(endTaskDirection>0);
        endTaskDirection+=getHeading();
        if (turnRight) stepTurn(Parameters.Direction.RIGHT);
        else stepTurn(Parameters.Direction.LEFT);
        sendLogMessage("Turning point. Waza!");
      } else {
        endTaskCounter--;
        if (Math.random()<0.1) {
          for (IRadarResult r : radarResults) {
            if (r.getObjectType()==IRadarResult.Types.OpponentMainBot) {
              fire(r.getObjectDirection());
              lastSeenDirection=r.getObjectDirection();
              return;
            }
          }
          fire(lastSeenDirection);
          return;
        } else {
          if (back) moveBack(); else move();
        }
      }*/
      if (berzerkInerty>50) {
        turnTask=true;
        moveTask=false;
        berzerk=false;
        endTaskDirection=(Math.random()-0.5)*2*Math.PI;
        turnRight=(endTaskDirection>0);
        endTaskDirection+=getHeading();
        if (turnRight) stepTurn(Parameters.Direction.RIGHT);
        else stepTurn(Parameters.Direction.LEFT);
        sendLogMessage("Turning point. Waza!");
        return;
      }
      if (endTaskCounter<0) {
        for (IRadarResult r : radarResults) {
          if (r.getObjectType()==IRadarResult.Types.OpponentMainBot) {
            fire(r.getObjectDirection());
            lastSeenDirection=r.getObjectDirection();
            berzerkInerty=0;
            return;
          }
        }
        fire(lastSeenDirection);
        berzerkInerty++;
        endTaskCounter=21;
        return;
      } else {
        endTaskCounter--;
        for (IRadarResult r : radarResults) {
          if (r.getObjectType()==IRadarResult.Types.OpponentMainBot) {
            lastSeenDirection=r.getObjectDirection();
            berzerkInerty=0;
            move();
            return;
          }
        }
        berzerkInerty++;
        move();
      }
      return;
    }
    if (radarResults.size()!=0){
      for (IRadarResult r : radarResults) {
        if (r.getObjectType()==IRadarResult.Types.OpponentMainBot) {
          berzerk=true;
          back=(Math.cos(getHeading()-r.getObjectDirection())>0);
          endTaskCounter=21;
          fire(r.getObjectDirection());
          lastSeenDirection=r.getObjectDirection();
          berzerkTurning=true;
          endTaskDirection=lastSeenDirection;
          double ref=endTaskDirection-getHeading();
          if (ref<0) ref+=Math.PI*2;
          turnRight=(ref>0 && ref<Math.PI);
          return;
        }
      }
      for (IRadarResult r : radarResults) {
        if (r.getObjectType()==IRadarResult.Types.OpponentSecondaryBot) {
          fire(r.getObjectDirection());
          return;
        }
      }
    }
    if (turnTask) {
      if (isHeading(endTaskDirection)) {
        if (firstMove) {
          firstMove=false;
  	  turnTask=false;
          moveTask=true;
          endTaskCounter=400;
	  move();
          sendLogMessage("Moving a head. Waza!");
          return;
        }
	turnTask=false;
        moveTask=true;
        endTaskCounter=100;
	move();
        sendLogMessage("Moving a head. Waza!");
      } else {
        if (turnRight) stepTurn(Parameters.Direction.RIGHT);
        else stepTurn(Parameters.Direction.LEFT);
      }
      return;
    }
    if (moveTask) {
      /*if (detectFront()!=NOTHING) {
        turnTask=true;
        moveTask=false;
        endTaskDirection=(Math.random()-0.5)*Math.PI;
        turnRight=(endTaskDirection>0);
        endTaskDirection+=getHeading();
        if (turnRight) stepTurn(Parameters.Direction.RIGHT);
        else stepTurn(Parameters.Direction.LEFT);
        sendLogMessage("Turning point. Waza!");
      }*/
      if (endTaskCounter<0) {
        turnTask=true;
        moveTask=false;
        endTaskDirection=(Math.random()-0.5)*2*Math.PI;
        turnRight=(endTaskDirection>0);
        endTaskDirection+=getHeading();
        if (turnRight) stepTurn(Parameters.Direction.RIGHT);
        else stepTurn(Parameters.Direction.LEFT);
        sendLogMessage("Turning point. Waza!");
      } else {
        endTaskCounter--;
        move();
      }
      return;
    }
    return;
  }
  private boolean isHeading(double dir){
    return Math.abs(Math.sin(getHeading()-dir))<Parameters.teamAMainBotStepTurnAngle;
  }
}

/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/Stage1.java 2014-10-18 buixuan.
 * ******************************************************/
package algorithms.exemples;

import robotsimulator.Brain;
import characteristics.Parameters;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;

import java.util.ArrayList;

public class Stage7MainA extends Brain {
  //---PARAMETERS---//
  private static final double ANGLEPRECISION = 0.01;
  private static final double FIREANGLEPRECISION = Math.PI/(double)6;

  private static final int ALPHA = 0x1EADDA;
  private static final int BETA = 0x5EC0;
  private static final int GAMMA = 0x333;
  private static final int TEAM = 0xBADDAD;
  private static final int UNDEFINED = 0xBADC0DE0;
  
  private static final int FIRE = 0xB52;
  private static final int FALLBACK = 0xFA11BAC;
  private static final int ROGER = 0x0C0C0C0C;
  private static final int OVER = 0xC00010FF;

  private static final int TURNSOUTHTASK = 1;
  private static final int MOVETASK = 2;
  private static final int TURNLEFTTASK = 3;
  private static final int SINK = 0xBADC0DE1;

  //---VARIABLES---//
  private int state;
  private double oldAngle;
  private double myX,myY;
  private boolean isMoving;
  private int whoAmI;
  private int fireRythm,rythm,counter;
  private int countDown;
  private double targetX,targetY;
  private boolean fireOrder;
  private boolean freeze;
  private boolean friendlyFire;

  //---CONSTRUCTORS---//
  public Stage7MainA() { super(); }

  //---ABSTRACT-METHODS-IMPLEMENTATION---//
  public void activate() {
    //ODOMETRY CODE
    whoAmI = GAMMA;
    for (IRadarResult o: detectRadar())
      if (isSameDirection(o.getObjectDirection(),Parameters.NORTH)) whoAmI=ALPHA;
    for (IRadarResult o: detectRadar())
      if (isSameDirection(o.getObjectDirection(),Parameters.SOUTH) && whoAmI!=GAMMA) whoAmI=BETA;
    if (whoAmI == GAMMA){
      myX=Parameters.teamAMainBot1InitX;
      myY=Parameters.teamAMainBot1InitY;
    } else {
      myX=Parameters.teamAMainBot2InitX;
      myY=Parameters.teamAMainBot2InitY;
    }
    if (whoAmI == ALPHA){
      myX=Parameters.teamAMainBot3InitX;
      myY=Parameters.teamAMainBot3InitY;
    }

    //INIT
    state=TURNSOUTHTASK;
    isMoving=false;
    fireOrder=false;
    fireRythm=0;
    oldAngle=myGetHeading();
    targetX=1500;
    targetY=1000;
  }
  public void step() {
    //ODOMETRY CODE
    if (isMoving){
      myX+=Parameters.teamAMainBotSpeed*Math.cos(myGetHeading());
      myY+=Parameters.teamAMainBotSpeed*Math.sin(myGetHeading());
      isMoving=false;
    }
    //DEBUG MESSAGE
    boolean debug=true;
    if (debug && whoAmI == ALPHA && state!=SINK) {
      sendLogMessage("#ALPHA *thinks* (x,y)= ("+(int)myX+", "+(int)myY+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"Â°. #State= "+state);
    }
    if (debug && whoAmI == BETA && state!=SINK) {
      sendLogMessage("#BETA *thinks* (x,y)= ("+(int)myX+", "+(int)myY+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"Â°. #State= "+state);
    }
    if (debug && whoAmI == GAMMA && state!=SINK) {
      sendLogMessage("#GAMMA *thinks* (x,y)= ("+(int)myX+", "+(int)myY+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"Â°. #State= "+state);
    }
    if (debug && fireOrder) sendLogMessage("Firing enemy!!");

    //COMMUNICATION
    ArrayList<String> messages=fetchAllMessages();
    for (String m: messages) if (Integer.parseInt(m.split(":")[1])==whoAmI || Integer.parseInt(m.split(":")[1])==TEAM) process(m);
    
    //RADAR DETECTION
    freeze=false;
    friendlyFire=true;
    for (IRadarResult o: detectRadar()){
      if (o.getObjectType()==IRadarResult.Types.OpponentMainBot || o.getObjectType()==IRadarResult.Types.OpponentSecondaryBot) {
        double enemyX=myX+o.getObjectDistance()*Math.cos(o.getObjectDirection());
        double enemyY=myY+o.getObjectDistance()*Math.sin(o.getObjectDirection());
        broadcast(whoAmI+":"+TEAM+":"+FIRE+":"+enemyX+":"+enemyY+":"+OVER);
      }
      if (o.getObjectDistance()<=100 && !isRoughlySameDirection(o.getObjectDirection(),getHeading()) && o.getObjectType()!=IRadarResult.Types.BULLET) {
        freeze=true;
      }
      if (o.getObjectType()==IRadarResult.Types.TeamMainBot || o.getObjectType()==IRadarResult.Types.TeamSecondaryBot || o.getObjectType()==IRadarResult.Types.Wreck) {
        if (fireOrder && onTheWay(o.getObjectDirection())) {
          friendlyFire=false;
        }
      }
    }
    if (freeze) return;

    //AUTOMATON
    if (fireOrder) countDown++;
    if (countDown>=100) fireOrder=false;
    if (fireOrder && fireRythm==0 && friendlyFire) {
      firePosition(targetX,targetY);
      fireRythm++;
      return;
    }
    fireRythm++;
    if (fireRythm>=Parameters.bulletFiringLatency) fireRythm=0;
    if (state==TURNSOUTHTASK && !(isSameDirection(getHeading(),Parameters.SOUTH))) {
      stepTurn(Parameters.Direction.RIGHT);
      return;
    }
    if (state==TURNSOUTHTASK && isSameDirection(getHeading(),Parameters.SOUTH)) {
      state=MOVETASK;
      myMove();
      return;
    }
    if (state==MOVETASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL) {
      myMove();
      return;
    }
    if (state==MOVETASK && detectFront().getObjectType()==IFrontSensorResult.Types.WALL) {
      state=TURNLEFTTASK;
      oldAngle=myGetHeading();
      stepTurn(Parameters.Direction.LEFT);
      return;
    }
    if (state==TURNLEFTTASK && !(isSameDirection(getHeading(),oldAngle+Parameters.LEFTTURNFULLANGLE))) {
      stepTurn(Parameters.Direction.LEFT);
      return;
    }
    if (state==TURNLEFTTASK && isSameDirection(getHeading(),oldAngle+Parameters.LEFTTURNFULLANGLE)) {
      state=MOVETASK;
      myMove();
      return;
    }



    if (state==0xB52){
      if (fireRythm==0) {
        firePosition(700,1500);
        fireRythm++;
        return;
      }
      fireRythm++;
      if (fireRythm==Parameters.bulletFiringLatency) fireRythm=0;
      if (rythm==0) stepTurn(Parameters.Direction.LEFT); else myMove();
      rythm++;
      if (rythm==14) rythm=0;
      return;
    }

    if (state==SINK) {
      myMove();
      return;
    }
    if (true) {
      return;
    }
  }
  private void myMove(){
    isMoving=true;
    move();
  }
  private double myGetHeading(){
    return normalizeRadian(getHeading());
  }
  private double normalizeRadian(double angle){
    double result = angle;
    while(result<0) result+=2*Math.PI;
    while(result>=2*Math.PI) result-=2*Math.PI;
    return result;
  }
  private boolean isSameDirection(double dir1, double dir2){
    return Math.abs(normalizeRadian(dir1)-normalizeRadian(dir2))<ANGLEPRECISION;
  }
  private boolean isRoughlySameDirection(double dir1, double dir2){
    return Math.abs(normalizeRadian(dir1)-normalizeRadian(dir2))<FIREANGLEPRECISION;
  }
  private void process(String message){
    if (Integer.parseInt(message.split(":")[2])==FIRE) {
      fireOrder=true;
      countDown=0;
      targetX=Double.parseDouble(message.split(":")[3]);
      targetY=Double.parseDouble(message.split(":")[4]);
    }
  }
  private void firePosition(double x, double y){
    if (myX<=x) fire(Math.atan((y-myY)/(double)(x-myX)));
    else fire(Math.PI+Math.atan((y-myY)/(double)(x-myX)));
    return;
  }
  private boolean onTheWay(double angle){
    if (myX<=targetX) return isRoughlySameDirection(angle,Math.atan((targetY-myY)/(double)(targetX-myX)));
    else return isRoughlySameDirection(angle,Math.PI+Math.atan((targetY-myY)/(double)(targetX-myX)));
  }
}
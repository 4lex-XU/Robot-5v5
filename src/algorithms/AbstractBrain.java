/* **
 
Simovies - Eurobot 2015 Robomovies Simulator.
Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
GPL version>=3 http://www.gnu.org/licenses/.
$Id: algorithms/Stage1.java 2014-10-18 buixuan.
**/
package algorithms;

import robotsimulator.Brain;

public class AbstractBrain extends Brain {

  private static final double ANGLEPRECISION = 0.001;

  //---VARIABLES---//
  protected int state;
  protected double oldAngle;
  protected double myX,myY;
  protected boolean isMoving;
  protected int whoAmI;

  //---CONSTRUCTORS---//
  public AbstractBrain() { super(); }

  //---ABSTRACT-METHODS-IMPLEMENTATION---//
  public void activate() {}

  public void step() {}

  protected void myMove(){
    isMoving=true;
    move();
  }
  protected boolean isSameDirection(double dir1, double dir2){
    return Math.abs(normalize(dir1)-normalize(dir2))<ANGLEPRECISION;
  }
  protected double normalize(double dir){
    double res=dir;
    while (res<0) res+=2*Math.PI;
    while (res>=2*Math.PI) res-=2*Math.PI;
    return res;
  }
  protected boolean isHeading(double dir){
    return Math.abs(Math.sin(getHeading()-dir))<ANGLEPRECISION;
  }
}
// Source code is decompiled from a .class file using FernFlower decompiler.
package algorithms.exemples;

import characteristics.IRadarResult;
import characteristics.IFrontSensorResult.Types;
import characteristics.Parameters.Direction;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import robotsimulator.Brain;

public class FifthElementMain extends Brain {
	
	
	class M1 implements Comparator<ArrayList<Double>> {
		FifthElementMain t;
	   M1(FifthElementMain t) {
	      this.t = t;
	   }

	   public int compare(ArrayList<Double> l1, ArrayList<Double> l2) {
	      if (l1.get(0) == l2.get(0)) {
	         return Double.compare(this.t.distance(this.t.myX, this.t.myY, (Double)l2.get(1), (Double)l2.get(2)), this.t.distance(this.t.myX, this.t.myY, (Double)l1.get(1), (Double)l1.get(2)));
	      } else if ((Double)l1.get(0) == -1.431633921E9) {
	         return this.t.distance(this.t.myX, this.t.myY, (Double)l1.get(1), (Double)l1.get(2)) <= 300.0 ? 1 : -1;
	      } else {
	         return this.t.distance(this.t.myX, this.t.myY, (Double)l2.get(1), (Double)l2.get(2)) > 300.0 ? 1 : -1;
	      }
	   }
	}
	   private boolean isLeftTeam = true;
	   private static final double ANGLEPRECISION = 0.001;
	   private static final double ANGLEPRECISIONBIS = 0.01;
	   private static final double DISTANCEPRECISION = 10.0;
	   private static final double SAMELINEPRECISION = 125.0;
	   private static final double MAIN = -1.431633921E9;
	   private static final double SECONDARY = -21846.0;
	   private static final int ROCKY = 2014683;
	   private static final int MARIO = 24269;
	   private static final int ALPHA = 2010586;
	   private static final int BETA = 24256;
	   private static final int GAMMA = 819;
	   private static final int TEAM = 12246445;
	   private static final int UNDEFINED = -1159983648;
	   private static final int FIRE = 2898;
	   private static final int POSITION = 32343;
	   private static final int OVER = -1073737473;
	   private static final int MOVETASK = 1;
	   private static final int STANDINGFIRINGTASK = 2;
	   private static final int BACKWARDFIRINGTASK = 3;
	   private static final int MOVEBACKTASK = 5;
	   private static final int TURNLEFTTASK = 6;
	   private static final int TURNRIGHTTASK = 7;
	   private static final int STARTINGTASK = 8;
	   private static final int HUNTINGTASK = 9;
	   private static final int TURNNORTHTASK = 10;
	   private static final int TURNSOUTHTASK = 11;
	   private static final int TURNEASTTASK = 12;
	   private static final int TURNWESTTASK = 13;
	   private static final int SINK = -1159983647;
	   private int state;
	   private int fireStep;
	   private double myX;
	   private double myY;
	   private boolean isMoving;
	   private int whoAmI;
	   private double targetX;
	   private double targetY;
	   private boolean fireOrder;
	   private Random rand;
	   private HashMap<Integer, ArrayList<Double>> allies = new HashMap();
	   private ArrayList<ArrayList<Double>> targets;
	   private int stepNumberLastFire;
	   private int stepNumber;
	   private int stepNumberMoveBack;
	   private double endTaskDirection;
	   private String huntingMode;
	   private boolean findNewShotAngle;
	   private boolean isMovingBack;
	   private int counter;

	   public FifthElementMain() {
	      ArrayList<Double> temp = new ArrayList(3);
	      temp.add(0.0);
	      temp.add(0.0);
	      temp.add(0.0);
	      this.allies.put(2010586, temp);
	      this.allies.put(24256, temp);
	      this.allies.put(819, temp);
	      this.allies.put(2014683, temp);
	      this.allies.put(24269, temp);
	      this.targets = new ArrayList(5);
	      this.rand = new Random();
	   }

	   public void activate() {
	      this.whoAmI = 819;
	      Iterator var1 = this.detectRadar().iterator();

	      IRadarResult o;
	      while(var1.hasNext()) {
	         o = (IRadarResult)var1.next();
	         if (this.isSameDirection(o.getObjectDirection(), -1.5707963267948966)) {
	            this.whoAmI = 2010586;
	         }
	      }

	      var1 = this.detectRadar().iterator();

	      while(var1.hasNext()) {
	         o = (IRadarResult)var1.next();
	         if (this.isSameDirection(o.getObjectDirection(), 1.5707963267948966) && this.whoAmI != 819) {
	            this.whoAmI = 24256;
	         }
	      }

	      this.isLeftTeam = true;
	      var1 = this.detectRadar().iterator();

	      while(var1.hasNext()) {
	         o = (IRadarResult)var1.next();
	         if (o.getObjectDirection() > 1.7278759594743864 && o.getObjectDirection() < 4.5553093477052) {
	            this.isLeftTeam = false;
	         }
	      }

	      if (!this.isLeftTeam) {
	         this.broadcast("663121:663121:663121:663121:663121:663121:663121");
	      }

	      if (this.isLeftTeam) {
	         if (this.whoAmI == 819) {
	            this.myX = 200.0;
	            this.myY = 800.0;
	         } else {
	            this.myX = 200.0;
	            this.myY = 1000.0;
	         }

	         if (this.whoAmI == 2010586) {
	            this.myX = 200.0;
	            this.myY = 1200.0;
	         }
	      } else {
	         if (this.whoAmI == 819) {
	            this.myX = 2800.0;
	            this.myY = 800.0;
	         } else {
	            this.myX = 2800.0;
	            this.myY = 1000.0;
	         }

	         if (this.whoAmI == 2010586) {
	            this.myX = 2800.0;
	            this.myY = 1200.0;
	         }
	      }

	      this.state = 8;
	      this.isMoving = false;
	      this.fireOrder = false;
	      this.targetX = 0.0;
	      this.targetY = 0.0;
	      this.stepNumberLastFire = 0;
	      this.stepNumber = 0;
	      this.stepNumberMoveBack = 0;
	      this.findNewShotAngle = false;
	      this.isMovingBack = false;
	      this.counter = 0;
	   }

	   public void step() {
	      ArrayList<String> messages = this.fetchAllMessages();
	      if (this.stepNumber == 0 && this.whoAmI == 24256) {
	         Iterator var2 = messages.iterator();

	         while(var2.hasNext()) {
	            String m = (String)var2.next();
	            if (Integer.parseInt(m.split(":")[0]) == 663121) {
	               this.isLeftTeam = false;
	               this.myX = 2800.0;
	               this.myY = 1000.0;
	            }
	         }
	      }

	      ++this.stepNumber;
	      if (this.stepNumber > 3000 && this.state == 8) {
	         this.state = 1;
	      }

	      if (this.counter > 460) {
	         this.counter = 0;
	      }

	      if (this.getHealth() == 0.0) {
	         this.state = -1159983647;
	      }

	      if (this.isMoving) {
	         this.myX += 1.0 * Math.cos(this.myGetHeading());
	         this.myY += 1.0 * Math.sin(this.myGetHeading());
	         this.realCoords();
	         this.isMoving = false;
	      }

	      if (this.isMovingBack) {
	         this.myX -= 1.0 * Math.cos(this.myGetHeading());
	         this.myY -= 1.0 * Math.sin(this.myGetHeading());
	         this.realCoords();
	         this.isMovingBack = false;
	      }

	      boolean debug = true;
	      int var10001;
	      if (debug && this.whoAmI == 2010586 && this.state != -1159983647) {
	         var10001 = (int)this.myX;
	         this.sendLogMessage("#ALPHA *thinks* (x,y)= (" + var10001 + ", " + (int)this.myY + ") theta= " + (int)(this.myGetHeading() * 180.0 / Math.PI) + "°. #State= " + this.state);
	      }

	      if (debug && this.whoAmI == 24256 && this.state != -1159983647) {
	         var10001 = (int)this.myX;
	         this.sendLogMessage("#BETA *thinks* (x,y)= (" + var10001 + ", " + (int)this.myY + ") theta= " + (int)(this.myGetHeading() * 180.0 / Math.PI) + "°. #State= " + this.state);
	      }

	      if (debug && this.whoAmI == 819 && this.state != -1159983647) {
	         var10001 = (int)this.myX;
	         this.sendLogMessage("#GAMMA *thinks* (x,y)= (" + var10001 + ", " + (int)this.myY + ") theta= " + (int)(this.myGetHeading() * 180.0 / Math.PI) + "°. #State= " + this.state);
	      }

	      if (debug && this.fireOrder) {
	         ++this.counter;
	         this.sendLogMessage("Firing enemy!!");
	      }

	      this.targets.clear();
	      Iterator var12 = messages.iterator();

	      while(true) {
	         String m;
	         do {
	            if (!var12.hasNext()) {
	               var10001 = this.whoAmI;
	               this.broadcast("" + var10001 + ":12246445:32343:" + this.myX + ":" + this.myY + ":" + this.getHeading() + ":-1073737473");
	               var12 = this.detectRadar().iterator();

	               IRadarResult o;
	               do {
	                  double distY;
	                  double distX;
	                  if (!var12.hasNext()) {
	                     if (this.fireOrder && !this.findNewShotAngle) {
	                        this.setTarget();
	                     }

	                     if (this.myX <= 50.0) {
	                        if (this.isHeading(0.0)) {
	                           this.state = 1;
	                           return;
	                        }

	                        this.state = 12;
	                        return;
	                     }

	                     if (this.myX >= 2950.0) {
	                        if (this.isHeading(Math.PI)) {
	                           this.state = 1;
	                           return;
	                        }

	                        this.state = 13;
	                        return;
	                     }

	                     if (this.myY <= 50.0) {
	                        if (this.isHeading(1.5707963267948966)) {
	                           this.state = 1;
	                           return;
	                        }

	                        this.state = 11;
	                        return;
	                     }

	                     if (this.myY >= 1950.0) {
	                        this.state = 10;
	                        if (this.isHeading(-1.5707963267948966)) {
	                           this.state = 1;
	                           return;
	                        }

	                        return;
	                     }

	                     if (this.state == 8) {
	                        if (this.stepNumber > 100 && this.canFireLatency()) {
	                           if (this.fireOrder && this.canIShot(this.targetX, this.targetY)) {
	                              this.firePosition(this.targetX, this.targetY);
	                              this.stepNumberLastFire = this.stepNumber;
	                              return;
	                           }

	                           this.fire(this.myGetHeading());
	                           this.stepNumberLastFire = this.stepNumber;
	                           return;
	                        }

	                        this.myMove();
	                     }

	                     if (this.fireOrder && this.canFireLatency() && this.canIShot(this.targetX, this.targetY)) {
	                        this.firePosition(this.targetX, this.targetY);
	                        this.stepNumberLastFire = this.stepNumber;
	                        return;
	                     }

	                     double ty;
	                     if (!this.fireOrder && this.stepNumber > 6000 && this.targets.size() > 0 && this.state != 9) {
	                        ty = (Double)((ArrayList)this.targets.get(0)).get(1);
	                        distY = (Double)((ArrayList)this.targets.get(0)).get(2);
	                        distX = Math.abs(ty - this.myX);
	                        double y1 = Math.abs(distY - this.myY);
	                        if (distX > y1 || y1 < 200.0) {
	                           this.state = 9;
	                           this.huntingMode = "x";
	                           return;
	                        }

	                        if (distX > 200.0) {
	                           this.state = 9;
	                           this.huntingMode = "y";
	                           return;
	                        }
	                     }

	                     if (this.state == 9 && !this.fireOrder) {
	                        if (this.targets.isEmpty()) {
	                           this.state = 1;
	                           this.huntingMode = "";
	                           return;
	                        }

	                        if (this.huntingMode.equals("x")) {
	                           ty = (Double)((ArrayList)this.targets.get(0)).get(1);
	                           distY = Math.abs(ty - this.myX);
	                           if (distY < 200.0) {
	                              this.huntingMode = "";
	                              this.state = 1;
	                              return;
	                           }

	                           if (ty < this.myX) {
	                              if (this.isHeading(Math.PI)) {
	                                 this.myMove();
	                                 return;
	                              }

	                              if (this.myGetHeading() < Math.PI && this.myGetHeading() > 0.0) {
	                                 this.stepTurn(Direction.RIGHT);
	                              } else {
	                                 this.stepTurn(Direction.LEFT);
	                              }

	                              return;
	                           }

	                           if (this.isHeading(0.0)) {
	                              this.myMove();
	                              return;
	                           }

	                           if (this.myGetHeading() < Math.PI && this.myGetHeading() > 0.0) {
	                              this.stepTurn(Direction.RIGHT);
	                           } else {
	                              this.stepTurn(Direction.LEFT);
	                           }

	                           return;
	                        }

	                        ty = (Double)((ArrayList)this.targets.get(0)).get(2);
	                        distY = Math.abs(ty - this.myY);
	                        if (distY < 200.0) {
	                           this.huntingMode = "";
	                           this.state = 1;
	                           return;
	                        }

	                        if (ty < this.myY) {
	                           if (this.isHeading(-1.5707963267948966)) {
	                              this.myMove();
	                              return;
	                           }

	                           if (!(this.myGetHeading() < 1.5707963267948966) && !(this.myGetHeading() > 4.71238898038469)) {
	                              this.stepTurn(Direction.RIGHT);
	                           } else {
	                              this.stepTurn(Direction.LEFT);
	                           }

	                           return;
	                        }

	                        if (this.isHeading(1.5707963267948966)) {
	                           this.myMove();
	                           return;
	                        }

	                        if (!(this.myGetHeading() < 1.5707963267948966) && !(this.myGetHeading() > 4.71238898038469)) {
	                           this.stepTurn(Direction.RIGHT);
	                        } else {
	                           this.stepTurn(Direction.LEFT);
	                        }

	                        return;
	                     }

	                     if (this.state == 9 && this.fireOrder) {
	                        this.state = 1;
	                        this.myMove();
	                        this.huntingMode = "";
	                        return;
	                     }

	                     if (this.state == 1 && this.detectFront().getObjectType() != Types.WALL) {
	                        if (this.canFireLatency()) {
	                           for(int i = 0; i < 10; ++i) {
	                              double angle = this.rand.nextDouble() * Math.PI / 6.0 - 0.2617993877991494;
	                              double x = this.myX + 1000.0 * Math.cos(this.myGetHeading() + angle);
	                              double y = this.myY + 1000.0 * Math.sin(this.myGetHeading() + angle);
	                              if (this.canIShot(x, y)) {
	                                 this.firePosition(x, y);
	                                 this.stepNumberLastFire = this.stepNumber;
	                                 return;
	                              }
	                           }
	                        }

	                        this.myMove();
	                        return;
	                     }

	                     if (this.state == 1 && this.detectFront().getObjectType() == Types.WALL) {
	                        if (this.myX > 2915.0 && this.myY > 1915.0 || this.myX > 2915.0 && this.myY < 85.0 || this.myX < 85.0 && this.myY < 85.0 || this.myX < 85.0 && this.myY > 1915.0) {
	                           this.state = 6;
	                           this.endTaskDirection = this.getHeading() + -1.5707963267948966;
	                           this.stepTurn(Direction.LEFT);
	                           return;
	                        }

	                        if (!(this.myX > 2915.0) && !(this.myX < 85.0)) {
	                           if (!(this.myY > 1915.0) && !(this.myY < 85.0)) {
	                              this.myMove();
	                              return;
	                           }

	                           if (!this.isHeading(-1.5707963267948966) && !this.isHeading(1.5707963267948966)) {
	                              this.myMove();
	                              return;
	                           }

	                           this.state = 6;
	                           this.endTaskDirection = this.getHeading() + -1.5707963267948966;
	                           this.stepTurn(Direction.LEFT);
	                           return;
	                        }

	                        if (!this.isHeading(0.0) && !this.isHeading(Math.PI)) {
	                           this.myMove();
	                           return;
	                        }

	                        this.state = 6;
	                        this.endTaskDirection = this.getHeading() + -1.5707963267948966;
	                        this.stepTurn(Direction.LEFT);
	                        return;
	                     }

	                     if (this.state == 2) {
	                        this.state = 1;
	                        if (++this.fireStep % 2 == 0 && this.counter < 415) {
	                           this.moveBack();
	                           this.myX -= 1.0 * Math.cos(this.getHeading());
	                           this.myY -= 1.0 * Math.sin(this.getHeading());
	                           this.realCoords();
	                        } else {
	                           this.myMove();
	                        }

	                        return;
	                     }

	                     if (this.state == 3) {
	                        this.state = 1;
	                        if (++this.fireStep % 1 == 0) {
	                           this.moveBack();
	                           this.myX -= 1.0 * Math.cos(this.getHeading());
	                           this.myY -= 1.0 * Math.sin(this.getHeading());
	                           this.realCoords();
	                        } else {
	                           this.myMove();
	                        }

	                        return;
	                     }

	                     if (this.state == 5) {
	                        if (this.stepNumber < this.stepNumberMoveBack + 25) {
	                           this.myMoveBack();
	                           return;
	                        }

	                        if (Math.random() < 0.5) {
	                           this.state = 6;
	                           this.endTaskDirection = this.getHeading() + -1.5707963267948966;
	                           this.stepTurn(Direction.LEFT);
	                        } else {
	                           this.state = 7;
	                           this.endTaskDirection = this.getHeading() + 1.5707963267948966;
	                           this.stepTurn(Direction.RIGHT);
	                        }

	                        return;
	                     }

	                     if (this.state == 7) {
	                        if (this.isHeading(this.endTaskDirection)) {
	                           this.state = 1;
	                           this.myMove();
	                        } else {
	                           this.stepTurn(Direction.RIGHT);
	                        }

	                        return;
	                     }

	                     if (this.state == 6) {
	                        if (this.isHeading(this.endTaskDirection)) {
	                           this.state = 1;
	                           this.myMove();
	                        } else {
	                           this.stepTurn(Direction.LEFT);
	                        }

	                        return;
	                     }

	                     if (this.state == 10 && !this.isHeading(-1.5707963267948966)) {
	                        if (!(this.myGetHeading() < 1.5707963267948966) && !(this.myGetHeading() > 4.71238898038469)) {
	                           this.stepTurn(Direction.RIGHT);
	                        } else {
	                           this.stepTurn(Direction.LEFT);
	                        }

	                        return;
	                     }

	                     if (this.state == 10 && this.isHeading(-1.5707963267948966)) {
	                        this.state = 1;
	                        this.myMove();
	                        return;
	                     }

	                     if (this.state == 11 && !this.isHeading(1.5707963267948966)) {
	                        if (!(this.myGetHeading() < 1.5707963267948966) && !(this.myGetHeading() > 4.71238898038469)) {
	                           this.stepTurn(Direction.LEFT);
	                        } else {
	                           this.stepTurn(Direction.RIGHT);
	                        }

	                        return;
	                     }

	                     if (this.state == 11 && this.isHeading(1.5707963267948966)) {
	                        this.state = 1;
	                        this.myMove();
	                        return;
	                     }

	                     if (this.state == 12 && !this.isHeading(0.0)) {
	                        if (this.myGetHeading() < Math.PI && this.myGetHeading() > 0.0) {
	                           this.stepTurn(Direction.LEFT);
	                        } else {
	                           this.stepTurn(Direction.RIGHT);
	                        }

	                        return;
	                     }

	                     if (this.state == 12 && this.isHeading(0.0)) {
	                        this.state = 1;
	                        this.myMove();
	                        return;
	                     }

	                     if (this.state == 13 && !this.isHeading(Math.PI)) {
	                        if (this.myGetHeading() < Math.PI && this.myGetHeading() > 0.0) {
	                           this.stepTurn(Direction.RIGHT);
	                        } else {
	                           this.stepTurn(Direction.LEFT);
	                        }

	                        return;
	                     }

	                     if (this.state == 13 && this.isHeading(Math.PI)) {
	                        this.state = 1;
	                        this.myMove();
	                        return;
	                     }

	                     if (this.state == -1159983647) {
	                        return;
	                     }

	                     return;
	                  }

	                  o = (IRadarResult)var12.next();
	                  if (o.getObjectType() == characteristics.IRadarResult.Types.OpponentMainBot || o.getObjectType() == characteristics.IRadarResult.Types.OpponentSecondaryBot) {
	                     distY = this.myX + o.getObjectDistance() * Math.cos(o.getObjectDirection());
	                     distX = this.myY + o.getObjectDistance() * Math.sin(o.getObjectDirection());
	                     var10001 = this.whoAmI;
	                     this.broadcast("" + var10001 + ":12246445:2898:" + (o.getObjectType() == characteristics.IRadarResult.Types.OpponentMainBot ? -1.431633921E9 : -21846.0) + ":" + distY + ":" + distX + ":-1073737473");
	                  }
	               } while(!(o.getObjectDistance() < 120.0) || o.getObjectType() == characteristics.IRadarResult.Types.BULLET || this.state != 1);

	               this.state = 5;
	               this.stepNumberMoveBack = this.stepNumber;
	               return;
	            }

	            m = (String)var12.next();
	         } while(Integer.parseInt(m.split(":")[1]) != this.whoAmI && Integer.parseInt(m.split(":")[1]) != 12246445);

	         this.process(m);
	      }
	   }

	   private void myMove() {
	      this.isMoving = true;
	      this.move();
	   }

	   private void myMoveBack() {
	      this.isMovingBack = true;
	      this.moveBack();
	   }

	   private double myGetHeading() {
	      return this.normalizeRadian(this.getHeading());
	   }

	   private double normalizeRadian(double angle) {
	      double result;
	      for(result = angle; result < 0.0; result += 6.283185307179586) {
	      }

	      while(result >= 6.283185307179586) {
	         result -= 6.283185307179586;
	      }

	      return result;
	   }

	   private boolean isSameDirection(double dir1, double dir2) {
	      return Math.abs(this.normalizeRadian(dir1) - this.normalizeRadian(dir2)) < 0.001;
	   }

	   private void process(String message) {
	      if (Integer.parseInt(message.split(":")[2]) == 2898) {
	         double x = Double.parseDouble(message.split(":")[4]);
	         double y = Double.parseDouble(message.split(":")[5]);
	         boolean already = false;
	         Iterator var7 = this.targets.iterator();

	         while(var7.hasNext()) {
	            ArrayList<Double> list = (ArrayList)var7.next();
	            if (Math.abs(x - (Double)list.get(1)) <= 10.0 && Math.abs(y - (Double)list.get(2)) <= 10.0) {
	               already = true;
	            }
	         }

	         if (!already) {
	            ArrayList<Double> target = new ArrayList(3);
	            target.add(Double.parseDouble(message.split(":")[3]));
	            target.add(x);
	            target.add(y);
	            this.targets.add(target);
	         }

	         this.fireOrder = true;
	      }

	      if (Integer.parseInt(message.split(":")[2]) == 32343) {
	         ArrayList<Double> temp = new ArrayList(2);
	         temp.add(Double.parseDouble(message.split(":")[3]));
	         temp.add(Double.parseDouble(message.split(":")[4]));
	         temp.add(Double.parseDouble(message.split(":")[5]));
	         this.allies.replace(Integer.parseInt(message.split(":")[0]), temp);
	      }

	   }

	   private void setTarget() {
	      ArrayList<ArrayList<Double>> realTargets = new ArrayList(5);
	      Iterator var2 = this.targets.iterator();

	      ArrayList target;
	      while(var2.hasNext()) {
	         target = (ArrayList)var2.next();
	         if (this.distance(this.myX, this.myY, (Double)target.get(1), (Double)target.get(2)) <= 1000.0) {
	            realTargets.add(target);
	         }
	      }

	      realTargets.sort(new M1(this));
	      var2 = realTargets.iterator();

	      do {
	         if (!var2.hasNext()) {
	            this.fireOrder = false;
	            return;
	         }

	         target = (ArrayList)var2.next();
	      } while(!this.canIShot((Double)target.get(1), (Double)target.get(2)));

	      this.targetX = (Double)target.get(1);
	      this.targetY = (Double)target.get(2);
	      if (this.distance(this.myX, this.myY, this.targetX, this.targetY) > 600.0) {
	         this.state = 2;
	      } else {
	         this.state = 3;
	      }

	   }

	   private void firePosition(double x, double y) {
	      if (this.myX <= x) {
	         this.fire(Math.atan((y - this.myY) / (x - this.myX)));
	      } else {
	         this.fire(Math.PI + Math.atan((y - this.myY) / (x - this.myX)));
	      }

	   }

	   private boolean canIShot(double x, double y) {
	      double a = (y - this.myY) / (x - this.myX);
	      double b = this.myY - a * this.myX;
	      Iterator var9 = this.allies.values().iterator();

	      double allyX;
	      double allyY;
	      do {
	         ArrayList ally;
	         do {
	            do {
	               do {
	                  if (!var9.hasNext()) {
	                     if (this.detectFront().getObjectType() != Types.TeamMainBot && this.detectFront().getObjectType() != Types.TeamSecondaryBot) {
	                        return true;
	                     }

	                     return false;
	                  }

	                  ally = (ArrayList)var9.next();
	               } while(this.distance(this.myX, this.myY, (Double)ally.get(0), (Double)ally.get(1)) <= 10.0);

	               double angleToAlly = this.getDirectionToTarget((Double)ally.get(0), (Double)ally.get(1));
	               double angleToTarget = this.getDirectionToTarget(x, y);
	               if (Math.abs(angleToAlly - angleToTarget) < 0.2617993877991494 && this.distance(this.myX, this.myY, (Double)ally.get(0), (Double)ally.get(1)) < this.distance(this.myX, this.myY, x, y)) {
	                  return false;
	               }

	               if (this.getHeading() == 0.0 && Math.abs((Double)ally.get(1) - this.myY) < 15.0 && (Double)ally.get(0) > this.myX) {
	                  return false;
	               }

	               if (this.getHeading() == Math.PI && Math.abs((Double)ally.get(1) - this.myY) < 15.0 && (Double)ally.get(0) < this.myX) {
	                  return false;
	               }

	               if (this.getHeading() == 1.5707963267948966 && Math.abs((Double)ally.get(0) - this.myX) < 15.0 && (Double)ally.get(0) > this.myX) {
	                  return false;
	               }

	               if (this.getHeading() == -1.5707963267948966 && Math.abs((Double)ally.get(0) - this.myX) < 15.0 && (Double)ally.get(0) < this.myX) {
	                  return false;
	               }

	               double allyA = Math.tan((Double)ally.get(2));
	               double allyB = (Double)ally.get(1) - allyA * (Double)ally.get(0);
	               allyX = (b - allyB) / (allyA - a);
	               allyY = a * allyX + b;
	            } while(!(this.distance((Double)ally.get(0), (Double)ally.get(1), allyX, allyY) <= 125.0));
	         } while((!(x >= allyX) || !(allyX >= this.myX)) && (!(x <= allyX) || !(allyX <= this.myX)));
	      } while((!(y >= allyY) || !(allyY >= this.myY)) && (!(y <= allyY) || !(allyY <= this.myY)));

	      return false;
	   }

	   private double distance(double x1, double y1, double x2, double y2) {
	      return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
	   }

	   private void realCoords() {
	      this.myX = this.myX < 0.0 ? 0.0 : this.myX;
	      this.myX = this.myX > 3000.0 ? 3000.0 : this.myX;
	      this.myY = this.myY < 0.0 ? 0.0 : this.myY;
	      this.myY = this.myY > 2000.0 ? 2000.0 : this.myY;
	   }

	   private boolean canFireLatency() {
	      return this.stepNumber > this.stepNumberLastFire + 20;
	   }

	   private boolean isHeading(double dir) {
	      return Math.abs(Math.sin(this.getHeading() - dir)) < 0.01;
	   }

	   private double getDirectionToTarget(double x, double y) {
	      double dir = Math.atan2(y - this.myY, x - this.myX);
	      return this.normalizeRadian(dir);
	   }
	}

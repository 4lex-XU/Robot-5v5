// Source code is decompiled from a .class file using FernFlower decompiler.
package algorithms.exemples;

import characteristics.IRadarResult;
import characteristics.IRadarResult.Types;
import characteristics.Parameters.Direction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import robotsimulator.Brain;

public class FifthElementSecondary extends Brain {
   private boolean isLeftTeam = true;
   private static final double ANGLEPRECISION = 0.001;
   private static final double ANGLEPRECISIONBIS = 0.01;
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
   private static final int TURNNORTHTASK = 1;
   private static final int TURNSOUTHTASK = 2;
   private static final int TURNEASTTASK = 3;
   private static final int TURNWESTTASK = 4;
   private static final int MOVETASK = 5;
   private static final int FIRSTMOVETASK = 6;
   private static final int FLEE = 7;
   private static final int TURNLEFTTASK = 8;
   private static final int MOVEBACKTASK = 9;
   private static final int TURNRIGHTTASK = 10;
   private static final int FIRSTTURNNORTHTASK = 11;
   private static final int FIRSTTURNSOUTHTASK = 22;
   private static final int SINK = -1159983647;
   private int state;
   private double myX;
   private double myY;
   private boolean isMoving;
   private int whoAmI;
   private HashMap<Integer, ArrayList<Double>> allies = new HashMap(5);
   private ArrayList<IRadarResult> ennemies;
   private double endTaskDirection;
   private int stepNumber;
   private int stepNumberMoveBack;
   private boolean isMovingBack;
   private boolean leftTeam;

   public FifthElementSecondary() {
      ArrayList<Double> temp = new ArrayList(2);
      temp.add(0.0);
      temp.add(0.0);
      this.allies.put(2010586, temp);
      this.allies.put(24256, temp);
      this.allies.put(819, temp);
      this.allies.put(2014683, temp);
      this.allies.put(24269, temp);
      this.ennemies = new ArrayList();
   }

   public void activate() {
      this.whoAmI = 2014683;
      Iterator var1 = this.detectRadar().iterator();

      IRadarResult o;
      while(var1.hasNext()) {
         o = (IRadarResult)var1.next();
         if (this.isSameDirection(o.getObjectDirection(), -1.5707963267948966)) {
            this.whoAmI = 24269;
         }
      }

      this.isLeftTeam = true;
      var1 = this.detectRadar().iterator();

      while(var1.hasNext()) {
         o = (IRadarResult)var1.next();
         if (this.isSameDirection(o.getObjectDirection(), 0.0)) {
            this.isLeftTeam = false;
         }
      }

      if (this.isLeftTeam) {
         if (this.whoAmI == 2014683) {
            this.myX = 500.0;
            this.myY = 800.0;
            this.state = 11;
         } else {
            this.myX = 500.0;
            this.myY = 1200.0;
            this.state = 22;
         }
      } else if (this.whoAmI == 2014683) {
         this.myX = 2500.0;
         this.myY = 800.0;
         this.state = 11;
      } else {
         this.myX = 2500.0;
         this.myY = 1200.0;
         this.state = 22;
      }

      if (this.myX == 2500.0) {
         this.leftTeam = false;
      } else {
         this.leftTeam = true;
      }

      this.isMoving = false;
      this.stepNumber = 0;
      this.stepNumberMoveBack = 0;
      this.isMovingBack = false;
   }

   public void step() {
      ++this.stepNumber;
      if (this.getHealth() == 0.0) {
         this.state = -1159983647;
      }

      if (this.isMoving) {
         this.myX += 3.0 * Math.cos(this.getHeading());
         this.myY += 3.0 * Math.sin(this.getHeading());
         this.realCoords();
         this.isMoving = false;
      }

      if (this.isMovingBack) {
         this.myX -= 1.0 * Math.cos(this.myGetHeading());
         this.myY -= 1.0 * Math.sin(this.myGetHeading());
         this.realCoords();
         this.isMovingBack = false;
      }

      int var10001;
      if (this.whoAmI == 2014683) {
         var10001 = (int)this.myX;
         this.sendLogMessage("#ROCKY *thinks* he is rolling at position (" + var10001 + ", " + (int)this.myY + ").#state:" + this.state);
      } else {
         var10001 = (int)this.myX;
         this.sendLogMessage("#MARIO *thinks* he is rolling at position (" + var10001 + ", " + (int)this.myY + ").#state:" + this.state);
      }

      Iterator var1 = this.detectRadar().iterator();

      while(true) {
         IRadarResult o;
         do {
            if (!var1.hasNext()) {
               var10001 = this.whoAmI;
               this.broadcast("" + var10001 + ":12246445:32343:" + this.myX + ":" + this.myY + ":" + this.myGetHeading() + ":-1073737473");
               this.ennemies.clear();
               var1 = this.detectRadar().iterator();

               while(var1.hasNext()) {
                  o = (IRadarResult)var1.next();
                  if (o.getObjectType() == Types.OpponentMainBot && o.getObjectDistance() <= 400.0 || o.getObjectType() == Types.OpponentSecondaryBot && o.getObjectDistance() <= 350.0) {
                     this.ennemies.add(o);
                     if (this.state == 5) {
                        this.state = 7;
                     }
                  }

                  if (o.getObjectDistance() < 120.0 && o.getObjectType() != Types.BULLET && this.state == 5) {
                     this.state = 9;
                     this.stepNumberMoveBack = this.stepNumber;
                  }
               }

               if (this.myX <= 50.0) {
                  if (this.isHeading(0.0)) {
                     this.state = 5;
                     return;
                  }

                  this.state = 3;
                  return;
               }

               if (this.myX >= 2950.0) {
                  if (this.isHeading(Math.PI)) {
                     this.state = 5;
                     return;
                  }

                  this.state = 4;
                  return;
               }

               if (this.myY <= 50.0) {
                  if (this.isHeading(1.5707963267948966)) {
                     this.state = 5;
                     return;
                  }

                  this.state = 2;
                  return;
               }

               if (this.myY >= 1950.0) {
                  this.state = 1;
                  if (this.isHeading(-1.5707963267948966)) {
                     this.state = 5;
                     return;
                  }

                  return;
               }

               if (this.state == 6) {
                  this.myMove();
                  if (this.whoAmI == 24269) {
                     if (this.myY > 1800.0) {
                        if (this.leftTeam) {
                           this.state = 3;
                        } else {
                           this.state = 4;
                        }

                        return;
                     }
                  } else if (this.myY < 500.0) {
                     if (this.leftTeam) {
                        this.state = 3;
                     } else {
                        this.state = 4;
                     }

                     return;
                  }
               }

               if (this.state == 11 && !this.isHeading(-1.5707963267948966)) {
                  if (!(this.myGetHeading() < 1.5707963267948966) && !(this.myGetHeading() > 4.71238898038469)) {
                     this.stepTurn(Direction.RIGHT);
                  } else {
                     this.stepTurn(Direction.LEFT);
                  }

                  return;
               }

               if (this.state == 11 && this.isHeading(-1.5707963267948966)) {
                  this.state = 6;
                  this.myMove();
                  return;
               }

               if (this.state == 22 && !this.isHeading(1.5707963267948966)) {
                  if (!(this.myGetHeading() < 1.5707963267948966) && !(this.myGetHeading() > 4.71238898038469)) {
                     this.stepTurn(Direction.LEFT);
                  } else {
                     this.stepTurn(Direction.RIGHT);
                  }

                  return;
               }

               if (this.state == 22 && this.isHeading(1.5707963267948966)) {
                  this.state = 6;
                  this.myMove();
                  return;
               }

               if (this.state == 1 && !this.isHeading(-1.5707963267948966)) {
                  if (!(this.myGetHeading() < 1.5707963267948966) && !(this.myGetHeading() > 4.71238898038469)) {
                     this.stepTurn(Direction.RIGHT);
                  } else {
                     this.stepTurn(Direction.LEFT);
                  }

                  return;
               }

               if (this.state == 1 && this.isHeading(-1.5707963267948966)) {
                  this.state = 5;
                  this.myMove();
                  return;
               }

               if (this.state == 2 && !this.isHeading(1.5707963267948966)) {
                  if (!(this.myGetHeading() < 1.5707963267948966) && !(this.myGetHeading() > 4.71238898038469)) {
                     this.stepTurn(Direction.LEFT);
                  } else {
                     this.stepTurn(Direction.RIGHT);
                  }

                  return;
               }

               if (this.state == 2 && this.isHeading(1.5707963267948966)) {
                  this.state = 5;
                  this.myMove();
                  return;
               }

               if (this.state == 3 && !this.isHeading(0.0)) {
                  if (this.myGetHeading() < Math.PI && this.myGetHeading() > 0.0) {
                     this.stepTurn(Direction.LEFT);
                  } else {
                     this.stepTurn(Direction.RIGHT);
                  }

                  return;
               }

               if (this.state == 3 && this.isHeading(0.0)) {
                  this.state = 5;
                  this.myMove();
                  return;
               }

               if (this.state == 4 && !this.isHeading(Math.PI)) {
                  if (this.myGetHeading() < Math.PI && this.myGetHeading() > 0.0) {
                     this.stepTurn(Direction.RIGHT);
                  } else {
                     this.stepTurn(Direction.LEFT);
                  }

                  return;
               }

               if (this.state == 4 && this.isHeading(Math.PI)) {
                  this.state = 5;
                  this.myMove();
                  return;
               }

               if (this.state == 5) {
                  if (this.detectFront().getObjectType() == characteristics.IFrontSensorResult.Types.WALL) {
                     if (this.whoAmI != 24269) {
                        this.state = 8;
                        this.endTaskDirection = this.getHeading() + -1.5707963267948966;
                        this.stepTurn(Direction.LEFT);
                        return;
                     }

                     if ((!(this.myX > 2800.0) || !(this.myY > 1800.0)) && (!(this.myX > 2800.0) || !(this.myY < 200.0)) && (!(this.myX < 200.0) || !(this.myY < 200.0)) && (!(this.myX < 200.0) || !(this.myY > 1800.0))) {
                        if (!(this.myX > 2800.0) && !(this.myX < 200.0)) {
                           if (!(this.myY > 1800.0) && !(this.myY < 200.0)) {
                              this.myMove();
                              return;
                           }

                           if (!this.isHeading(-1.5707963267948966) && !this.isHeading(1.5707963267948966)) {
                              this.myMove();
                              return;
                           }

                           this.state = 8;
                           this.endTaskDirection = this.getHeading() + -1.5707963267948966;
                           this.stepTurn(Direction.LEFT);
                           return;
                        }

                        if (!this.isHeading(0.0) && !this.isHeading(Math.PI)) {
                           this.myMove();
                           return;
                        }

                        this.state = 8;
                        this.endTaskDirection = this.getHeading() + -1.5707963267948966;
                        this.stepTurn(Direction.LEFT);
                        return;
                     }

                     this.state = 8;
                     this.endTaskDirection = this.getHeading() + -1.5707963267948966;
                     this.stepTurn(Direction.LEFT);
                     return;
                  }

                  this.myMove();
                  return;
               }

               if (this.state == 10) {
                  if (this.isHeading(this.endTaskDirection)) {
                     this.state = 5;
                     this.myMove();
                  } else {
                     this.stepTurn(Direction.RIGHT);
                  }

                  return;
               }

               if (this.state == 8) {
                  if (this.isHeading(this.endTaskDirection)) {
                     this.state = 5;
                     this.myMove();
                  } else {
                     this.stepTurn(Direction.LEFT);
                  }

                  return;
               }

               if (this.state == 9) {
                  if (this.stepNumber < this.stepNumberMoveBack + 25) {
                     this.myMoveBack();
                     return;
                  }

                  if (Math.random() < 0.5) {
                     this.state = 8;
                     this.endTaskDirection = this.getHeading() + -1.5707963267948966;
                     this.stepTurn(Direction.LEFT);
                  } else {
                     this.state = 10;
                     this.endTaskDirection = this.getHeading() + 1.5707963267948966;
                     this.stepTurn(Direction.RIGHT);
                  }

                  return;
               }

               if (this.state != 7) {
                  if (this.state == -1159983647) {
                     return;
                  }

                  return;
               }

               if ((this.myX > 2900.0 || this.myX < 100.0) && (this.myY > 1900.0 || this.myX < 100.0)) {
                  this.state = 10;
                  this.endTaskDirection = this.getHeading() + 1.5707963267948966;
                  this.stepTurn(Direction.RIGHT);
                  return;
               }

               if (!(this.myX > 2900.0) && !(this.myX < 100.0)) {
                  if (!(this.myY > 1900.0) && !(this.myY < 100.0)) {
                     this.moveBack();
                     this.myX -= 3.0 * Math.cos(this.getHeading());
                     this.myY -= 3.0 * Math.sin(this.getHeading());
                     this.realCoords();
                     if (this.ennemies.isEmpty()) {
                        this.state = 5;
                     }

                     return;
                  }

                  if (!this.isHeading(-1.5707963267948966) && !this.isHeading(1.5707963267948966)) {
                     this.moveBack();
                     this.myX -= 3.0 * Math.cos(this.getHeading());
                     this.myY -= 3.0 * Math.sin(this.getHeading());
                     this.realCoords();
                     if (this.ennemies.isEmpty()) {
                        this.state = 5;
                     }

                     return;
                  }

                  this.state = 10;
                  this.endTaskDirection = this.getHeading() + 1.5707963267948966;
                  this.stepTurn(Direction.RIGHT);
                  return;
               }

               if (!this.isHeading(0.0) && !this.isHeading(Math.PI)) {
                  this.moveBack();
                  this.myX -= 3.0 * Math.cos(this.getHeading());
                  this.myY -= 3.0 * Math.sin(this.getHeading());
                  this.realCoords();
                  if (this.ennemies.isEmpty()) {
                     this.state = 5;
                  }

                  return;
               }

               this.state = 10;
               this.endTaskDirection = this.getHeading() + 1.5707963267948966;
               this.stepTurn(Direction.RIGHT);
               return;
            }

            o = (IRadarResult)var1.next();
         } while(o.getObjectType() != Types.OpponentMainBot && o.getObjectType() != Types.OpponentSecondaryBot);

         double enemyX = this.myX + o.getObjectDistance() * Math.cos(o.getObjectDirection());
         double enemyY = this.myY + o.getObjectDistance() * Math.sin(o.getObjectDirection());
         var10001 = this.whoAmI;
         this.broadcast("" + var10001 + ":12246445:2898:" + (o.getObjectType() == Types.OpponentMainBot ? -1.431633921E9 : -21846.0) + ":" + enemyX + ":" + enemyY + ":-1073737473");
         this.ennemies.add(o);
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
      return Math.abs(dir1 - dir2) < 0.001;
   }

   private boolean isHeading(double dir) {
      return Math.abs(Math.sin(this.getHeading() - dir)) < 0.01;
   }

   private void realCoords() {
      this.myX = this.myX < 0.0 ? 0.0 : this.myX;
      this.myX = this.myX > 3000.0 ? 3000.0 : this.myX;
      this.myY = this.myY < 0.0 ? 0.0 : this.myY;
      this.myY = this.myY > 2000.0 ? 2000.0 : this.myY;
   }
}

package algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import algorithms.MacDuoBaseBot.State;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.IRadarResult.Types;
import characteristics.Parameters;
import robotsimulator.Brain;

class BotState {
	private Position position = new Position(0, 0);
	private boolean isAlive = true;

	public BotState() {}
	public BotState(double x, double y, boolean alive) {
		position.setX(x);
		position.setY(y);
		isAlive = alive;
	}

	public void setPosition(double x, double y) {
		position.setX(x);
		position.setY(y);
	}
	public Position getPosition() {return position;}
	public void setAlive(boolean alive) {isAlive = alive;}
	public boolean isAlive() {return isAlive;}
}

class Position {
	private double x;
	private double y;
	
	public Position(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	public void setX(double x) {this.x = x;}
	public void setY(double y) {this.y = y;}
	public double getX() {return x;}
	public double getY() {return y;}
	public String toString() { return "X : " + x + "; Y : " + y;}

	@Override
	public int hashCode() {
		return Objects.hash(x, y);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Position other = (Position) obj;
		return Double.doubleToLongBits(x) == Double.doubleToLongBits(other.x)
				&& Double.doubleToLongBits(y) == Double.doubleToLongBits(other.y);
	}
	
}

class Segment {
    public Position start;
    public Position end;
    public Segment(Position start, Position end) {
        this.start = start;
        this.end = end;
    }
}



//====================================================================================
//====================================ABSTRACT BOT====================================
//====================================================================================

abstract class MacDuoBaseBot extends Brain {

	protected static final String NBOT = "NBOT";
    protected static final String SBOT = "SBOT";
	protected static final String MAIN1 = "1";
	protected static final String MAIN2 = "2";
	protected static final String MAIN3 = "3";
	
	protected static final double ANGLEPRECISION = 0.001;
	protected enum State {FIRST_RDV, MOVING, MOVING_BACK, TURNING_LEFT, TURNING_RIGHT, FIRE, DEAD };
	
	protected final double BOT_RADIUS = 50;
	protected final double BULLET_RADIUS = 5;

	protected String whoAmI;
	
	 //---VARIABLES---//
	protected Position myPos;
	protected boolean freeze;
    protected boolean isTeamA;
	protected boolean rdv_point;
	protected boolean turningTask = false;
	protected State state;
	protected double oldAngle;
	protected double obstacleDirection = 0;
	protected Parameters.Direction turnedDirection;
	protected double targetX, targetY;
	protected boolean isShooterAvoiding; 
	protected double rdvXToReach, rdvYToReach;

	
	
	protected Map<String, BotState> allyPos = new HashMap<>();	// Stocker la position des alliés
	protected Map<Position, IRadarResult.Types> oppPos = new HashMap<>();	// Stocker la position des opposants
	protected Map<String, Double[]> wreckPos = new HashMap<>();	// Stocker la position des débris
	
    private int avoidanceTimer = 0;
    private static final int AVOIDANCE_DURATION = 10;
 
	
	public MacDuoBaseBot() {
		super();
		allyPos.put(NBOT, new BotState());
		allyPos.put(SBOT, new BotState());
		allyPos.put(MAIN1, new BotState());
		allyPos.put(MAIN2, new BotState());
		allyPos.put(MAIN3, new BotState());
	}
	protected abstract void detection();
	
	protected void reach_rdv_point(double tX, double tY) {
	    double angleToTarget = Math.atan2(tY - myPos.getY(), tX - myPos.getX());
	    //System.out.println(" ANGLE TO TARGET " + angleToTarget + " BOTTT " + whoAmI + " MY HEADINGGG " + getHeading());

	    // Calculer la distance
	    double distanceToScout = Math.sqrt(Math.pow(myPos.getX() - tX, 2) + Math.pow(myPos.getY() - tY, 2));

	    // Vérifier si on est dans la portée du scout
	    if (whoAmI==NBOT || whoAmI==SBOT || distanceToScout > 450) {

		    // Adapter la direction aux angles cardinaux et diagonaux
		    angleToTarget = getNearestAllowedDirection(angleToTarget);
	    	System.out.println("TEAMMMMMMM AAAAAAA "+ isTeamA + " " + whoAmI + " to the angle "+ angleToTarget);

		    if (!isSameDirection(getHeading(), angleToTarget)) {
		    	System.out.println(" SEARCHINGGGGG ANGLEEE " + angleToTarget);
		        turnTo(angleToTarget);
		    } else {
		        myMove(true);
		        System.out.println("followinggg " + whoAmI + " X " + tX + " y " + tY);
		    }

	    }
	}
	
	// StepTurn Gauche ou Droite, selon un angle cible
	protected void turnTo(double targetAngle) {
	    double currentAngle = getHeading();
	    double diff = normalize(targetAngle - currentAngle);
	    if (diff > Math.PI) {
	        diff -= 2 * Math.PI;  
	    } else if (diff < -Math.PI) {
	        diff += 2 * Math.PI; 
	    }
	    if (diff > ANGLEPRECISION) {
	        stepTurn(Parameters.Direction.RIGHT);
	    } else if (diff < -ANGLEPRECISION) {
	        stepTurn(Parameters.Direction.LEFT);
	    }
	}
	
	//=========================================BASE=========================================

	protected boolean isSameDirection(double dir1, double dir2) {
		double diff = Math.abs(normalize(dir1) - normalize(dir2));
		return diff < ANGLEPRECISION;
	}
	
	protected boolean isRoughlySameDirection(double dir1, double dir2) {
		double diff = Math.abs(normalize(dir1) - normalize(dir2));
		return diff < 0.5;
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
	
	protected double myGetHeading(){
	    return normalize(getHeading());
	  }
	
	protected void sendMyPosition() {
		broadcast("POS "+whoAmI+" "+myPos.getX()+" "+myPos.getY());
	}
	
	protected void turnLeft() {
		
		if (!isSameDirection(getHeading(),oldAngle+(-0.5 * Math.PI))) {
			//System.out.println("trying to turn left");
			stepTurn(Parameters.Direction.LEFT);
	    } else {
	    	//System.out.println("trying to move left");
	        state = State.MOVING;
	        myMove(true);
	    }				
	}
	
	protected void turnRight() {
		if (!isSameDirection(getHeading(),oldAngle+(0.5 * Math.PI))) {
            stepTurn(Parameters.Direction.RIGHT);
            //if (whoAmI == SBOT) System.out.println("turnED RIGHTTTTTT " + whoAmI);
	    } else {
	    	//if (whoAmI == SBOT) System.out.println(getHeading()+ " "+ oldAngle+(0.5 * Math.PI));
	        state = State.MOVING;
	        myMove(true);
	    }				
	}
	
	protected double distance(Position p1, Position p2) {
	    return Math.sqrt(Math.pow(p2.getX() - p1.getX(), 2) + Math.pow(p2.getY() - p1.getY(), 2));
	}
	
	protected double getCurrentSpeed() throws Exception {
		switch (whoAmI) {
			case NBOT:
			case SBOT: 
				return Parameters.teamASecondaryBotSpeed;
			case MAIN1:
			case MAIN2:
			case MAIN3:
				return Parameters.teamAMainBotSpeed;
			default :
				throw new Exception("Pas de vitesse pour ce robot");
		}
	}
	
	/**
	 * Cette méthode ajuste l'angle vers la direction la plus proche parmi :
	 * Nord, Sud, Est, Ouest, Nord-Est, Nord-Ouest, Sud-Est, Sud-Ouest
	 */
	protected double getNearestAllowedDirection(double angle) {
	    double[] allowedAngles = {
	        0,                      // Est (0°)
	        //Math.PI / 4,            // Nord-Est (45°)
	        Math.PI / 2,            // Nord (90°)
	        //3 * Math.PI / 4,        // Nord-Ouest (135°)
	        Math.PI,                // Ouest (180°)
	        //-3 * Math.PI / 4,       // Sud-Ouest (-135°)
	        -Math.PI / 2,           // Sud (-90°)
	        //-Math.PI / 4            // Sud-Est (-45°)
	    };

	    double bestAngle = allowedAngles[0];
	    double minDiff = Math.abs(normalize(angle) - normalize(bestAngle));

	    for (double allowed : allowedAngles) {
	        double diff = Math.abs(normalize(angle) - normalize(allowed));
	        if (diff < minDiff) {
	            bestAngle = allowed;
	            minDiff = diff;
	        }
	    }
	    return bestAngle;
	}

	protected boolean isPointInTrajectory(double robotX, double robotY, double robotHeading, double pointX, double pointY) {
		// Dimensions de la zone
		double pathLength = BOT_RADIUS*2.5; // Longueur de la trajectoire
		double pathWidth = BOT_RADIUS*2;   // Largeur totale (50 mm de chaque côté)
	
		// Calcul du vecteur direction du robot
		double dirX = Math.cos(robotHeading);
		double dirY = Math.sin(robotHeading);
	
		// Calcul des coins de la zone
		double halfWidth = pathWidth / 2;
	
		// Centre avant et arrière
		double frontX = robotX + pathLength * dirX;
		double frontY = robotY + pathLength * dirY;
		double backX = robotX;
		double backY = robotY;
	
		// Vecteur perpendiculaire pour déterminer la largeur de la zone
		double perpX = -dirY;
		double perpY = dirX;
	
		// Coins du rectangle de la trajectoire
		double ALX = backX + halfWidth * perpX;  // Arrière gauche
		double ALY = backY + halfWidth * perpY;
		double ARX = backX - halfWidth * perpX;  // Arrière droit
		double ARY = backY - halfWidth * perpY;
		double PLX = frontX + halfWidth * perpX; // Avant gauche
		double PLY = frontY + halfWidth * perpY;
		double PRX = frontX - halfWidth * perpX; // Avant droit
		double PRY = frontY - halfWidth * perpY;
	
		// Vérifier si le point est dans le rectangle
		return isPointInRectangle(pointX, pointY, ALX, ALY, ARX, ARY, PLX, PLY, PRX, PRY);
	}

	protected boolean isPointInRectangle(double Px, double Py, double ALX, double ALY, double ARX, double ARY, 
                                     double PLX, double PLY, double PRX, double PRY) {
		// Produit scalaire pour vérifier si le point est dans la zone
		double APx = Px - ALX;
		double APy = Py - ALY;
		double ABx = ARX - ALX;
		double ABy = ARY - ALY;
		double ADx = PLX - ALX;
		double ADy = PLY - ALY;

		double dotAB = APx * ABx + APy * ABy;
		double dotAD = APx * ADx + APy * ADy;
		double dotAB_AB = ABx * ABx + ABy * ABy;
		double dotAD_AD = ADx * ADx + ADy * ADy;

		return (0 <= dotAB && dotAB <= dotAB_AB) && (0 <= dotAD && dotAD <= dotAD_AD);
	}
	
	protected Position[] getObstacleCorners(IRadarResult obstacle, double robotX, double robotY) {
		// Position du centre de l'obstacle
		double obstacleX = robotX + obstacle.getObjectDistance() * Math.cos(obstacle.getObjectDirection());
		double obstacleY = robotY + obstacle.getObjectDistance() * Math.sin(obstacle.getObjectDirection());
		double obstacleRadius = obstacle.getObjectRadius();
	
		// Calcul des coins du rectangle englobant
		Position topLeft = new Position(obstacleX - obstacleRadius, obstacleY + obstacleRadius);
		Position topRight = new Position(obstacleX + obstacleRadius, obstacleY + obstacleRadius);
		Position bottomLeft = new Position(obstacleX - obstacleRadius, obstacleY - obstacleRadius);
		Position bottomRight = new Position(obstacleX + obstacleRadius, obstacleY - obstacleRadius);
	
		return new Position[]{topLeft, topRight, bottomLeft, bottomRight};
	}
	
	protected Position[] getObstacleCorners(double obstacleRadius , double robotX, double robotY) {
		double distance = distance(myPos, new Position (robotX, robotY));
		double direction = Math.atan2(robotY - myPos.getY(), robotX - myPos.getX());
		// Position du centre de l'obstacle
		double obstacleX = robotX + distance * Math.cos(direction);
		double obstacleY = robotY + distance * Math.sin(direction);
	
		// Calcul des coins du rectangle englobant
		Position topLeft = new Position(obstacleX - obstacleRadius, obstacleY - obstacleRadius);
		Position topRight = new Position(obstacleX + obstacleRadius, obstacleY - obstacleRadius);
		Position bottomLeft = new Position(obstacleX - obstacleRadius, obstacleY + obstacleRadius);
		Position bottomRight = new Position(obstacleX + obstacleRadius, obstacleY + obstacleRadius);
	
		return new Position[]{topLeft, topRight, bottomLeft, bottomRight};
	}
	
	protected void initiateObstacleAvoidance() {
		isShooterAvoiding = true; 
		boolean obstacleInPathRight = false;
		boolean obstacleInPathLeft = false;
		oldAngle = myGetHeading();


		for (IRadarResult o : detectRadar()) {
			double oX = myPos.getX() + o.getObjectDistance() * Math.cos(o.getObjectDirection());
			double oY = myPos.getY() + o.getObjectDistance() * Math.sin(o.getObjectDirection());
			if (allyPos.get(whoAmI).isAlive() && o.getObjectType() != Types.BULLET) {
				for (Position p : getObstacleCorners(o, myPos.getX(), myPos.getY())) {
					if (!obstacleInPathRight) {
						obstacleInPathRight = isPointInTrajectory(myPos.getX(), myPos.getY(), (getHeading() + 0.5 * Math.PI), p.getX(), p.getY());
					}
					if (!obstacleInPathLeft) {
						if(whoAmI==SBOT)
							//System.out.println("obstacle in path left "+p.getX()+" "+p.getY()+ " " + o.getObjectType());
					    obstacleInPathLeft = isPointInTrajectory(myPos.getX(), myPos.getY(), (getHeading() - 0.5 * Math.PI), p.getX(), p.getY());
					}
			    }
			}
		}
		//if (whoAmI == MAIN1)  System.out.println("isShooterAvoiding: "+isShooterAvoiding + " obstacleInPathRight: "+obstacleInPathRight + " obstacleInPathLeft: "+obstacleInPathLeft);

		if (!obstacleInPathRight) {
			//if (whoAmI == MAIN1) System.out.println("isShooterAvoiding: "+isShooterAvoiding + " turning right");
			state = State.TURNING_RIGHT;
			targetX = myPos.getX() + Math.cos(getHeading()+0.5*Math.PI) *100;
			targetY= myPos.getY() + Math.sin(getHeading()+0.5*Math.PI) *100;

			return;
		}
		if (!obstacleInPathLeft) {
			//if (whoAmI == MAIN1) System.out.println("isShooterAvoiding: "+isShooterAvoiding + " turning left");
			state = State.TURNING_LEFT;
			targetX = myPos.getX() + Math.cos(getHeading()-0.5*Math.PI) *100;
			targetY= myPos.getY() + Math.sin(getHeading()-0.5*Math.PI) *100;
			return;
		}
		//sendLogMessage("move back");
		state = State.MOVING_BACK;
		targetX = myPos.getX() - Math.cos(getHeading()) *100;
		targetY= myPos.getY() - Math.sin(getHeading()) *100;
		return;
	}
	
	protected void myMove(boolean forward) {
		double speed = (whoAmI == NBOT || whoAmI == SBOT) ? Parameters.teamASecondaryBotSpeed : Parameters.teamAMainBotSpeed;

		if (forward) {
			double myPredictedX = myPos.getX() + Math.cos(getHeading()) * speed;
			double myPredictedY = myPos.getY() + Math.sin(getHeading()) * speed;

			// évite de se bloquer dans les murs
			if(myPredictedX > 100 && myPredictedX < 2900 && myPredictedY > 100 && myPredictedY < 1900 ) {
				//System.out.println("pas de mur");
				move(); 
				//System.out.println("moving forward " + whoAmI + "TEAMMM AAA " + isTeamA);
				myPos.setX(myPredictedX);
				myPos.setY(myPredictedY);
	    		sendMyPosition();
	    		return;
			}

		} else {
			double myPredictedX = myPos.getX() - Math.cos(getHeading()) * speed;
			double myPredictedY = myPos.getY() - Math.sin(getHeading()) * speed;

			// évite de se bloquer dans les murs
			if(myPredictedX > 100 && myPredictedX < 2900 && myPredictedY > 100 && myPredictedY < 1900 ) {
				moveBack(); 
				myPos.setX(myPredictedX);
				myPos.setY(myPredictedY);
	    		sendMyPosition();
	    		return;
			}
		}
		initiateObstacleAvoidance();
	}
	
	protected boolean hasReachedTarget(double targetX, double targetY, boolean movingForward) {
		boolean reachedX, reachedY;
		double currentX = myPos.getX();
		double currentY = myPos.getY();
		
		if (movingForward) {
		// Lorsque le robot avance, on s'attend à ce que les composantes 
		// suivent le signe de cos(avoidanceHeading) et sin(avoidanceHeading)
		if (Math.cos(getHeading()) > 0) {
		reachedX = (currentX >= targetX);
		} else if (Math.cos(getHeading()) < 0) {
		reachedX = (currentX <= targetX);
		} else {
		reachedX = true;
		}
		if (Math.sin(getHeading()) > 0) {
		reachedY = (currentY >= targetY);
		} else if (Math.sin(getHeading()) < 0) {
		reachedY = (currentY <= targetY);
		} else {
		reachedY = true;
		}
		} else { // movingBackward
		// Lorsqu'on recule, la direction effective est inversée
		if (Math.cos(getHeading()) > 0) {
		reachedX = (currentX <= targetX);
		} else if (Math.cos(getHeading()) < 0) {
		reachedX = (currentX >= targetX);
		} else {
		reachedX = true;
		}
		if (Math.sin(getHeading()) > 0) {
		reachedY = (currentY <= targetY);
		} else if (Math.sin(getHeading()) < 0) {
		reachedY = (currentY >= targetY);
		} else {
		reachedY = true;
		}
		}

		return reachedX && reachedY;
	}
	    
}

//=================================================================================
//====================================SECONDARY====================================
//=================================================================================

public class SecondaryMacDuo extends MacDuoBaseBot{	
    
    private int count;
    
    // points de rdv
    //private static final double TARGET_X1 = 600; // TargetX2 - 300
    //private static final double TARGET_Y1 = 840; // TargetY2 - 660
    //private static final double TARGET_X2 = 900; 
    //private static final double TARGET_Y2 = 1500;
    
    private double rdvX; // TargetX2 - 300
    private double rdvY; // TargetY2 - 660

    private boolean isShooterAround = false;
        
    private boolean obstacleDetected = false;
  
    private boolean firstTurning = true;

    private double avoidanceAngle = Math.PI/2;
    private static final double OBSTACLE_AVOIDANCE_DISTANCE = 150;
    
	//=========================================CORE=========================================	
	public SecondaryMacDuo() {super();}

	@Override
	public void activate() {
		isTeamA = (getHeading() == Parameters.EAST);

		//détermination de l'emplacement du bot
		whoAmI = NBOT;
		for (IRadarResult o: detectRadar()) {
			if (isSameDirection(o.getObjectDirection(),Parameters.NORTH)) whoAmI = SBOT;
		}
		if (whoAmI == NBOT){
			myPos = new Position((isTeamA ? Parameters.teamASecondaryBot1InitX : Parameters.teamBSecondaryBot1InitX),
								 (isTeamA ? Parameters.teamASecondaryBot1InitY : Parameters.teamBSecondaryBot1InitY));
	    } else {
			myPos = new Position((isTeamA ? Parameters.teamASecondaryBot2InitX : Parameters.teamBSecondaryBot2InitX),
								 (isTeamA ? Parameters.teamASecondaryBot2InitY : Parameters.teamBSecondaryBot2InitY));
	    }

	    //sendLogMessage(whoAmI+" activé, position : " + myPos.getX() + "," + myPos.getY());
		
	    //INIT
	    rdv_point=true;
	    state = State.FIRST_RDV;
	    oldAngle = myGetHeading();

    	rdvX = (isTeamA) ? 1000 : 1400;
		rdvY = (whoAmI == SBOT)? 1500 : 500;

	}

	@Override
	public void step() {
		
		//DEBUG MESSAGE
        boolean debug = true;

		detection();
		readMessages();
		
        if (debug && whoAmI == NBOT) {
        	sendLogMessage("#NBOT *thinks* (x,y)= ("+(int)myPos.getX()+", "+(int)myPos.getY()+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"°. #State= "+state);
        }
        if (debug && whoAmI == SBOT) {
        	sendLogMessage("#SBOT *thinks* (x,y)= ("+(int)myPos.getX()+", "+(int)myPos.getY()+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"°. #State= "+state);
        }
        
		if(rdv_point) {
			//System.out.println("REACHING "+ rdvX + " " + rdvY + " " + whoAmI);
			if (whoAmI == NBOT) {
				if (!isSameDirection(getHeading(), Parameters.NORTH) && firstTurning) {
					if (isTeamA) {
						stepTurn(Parameters.Direction.LEFT);
					} else stepTurn(Parameters.Direction.RIGHT);
					return;
				}
				if (myPos.getY() > rdvY) {
					firstTurning = false;
					myMove(true);
				} else if ((isTeamA && !isSameDirection(getHeading(), Parameters.EAST)) || (!isTeamA && !isSameDirection(getHeading(), Parameters.WEST))) {
					if (isTeamA) {
						stepTurn(Parameters.Direction.RIGHT);
					} else 	stepTurn(Parameters.Direction.LEFT);
					return;
				} else {
						rdv_point = false;
						state = State.MOVING;
					}
				return;
			} else {
				if (!isSameDirection(getHeading(), Parameters.SOUTH) && firstTurning) {
					if (isTeamA) {
						stepTurn(Parameters.Direction.RIGHT);
					} else 	stepTurn(Parameters.Direction.LEFT);
					return;
				}
				if (myPos.getY() < rdvY) {
					firstTurning = false;
					myMove(true);
				} else if ( (isTeamA && !isSameDirection(getHeading(), Parameters.EAST)) || (!isTeamA && !isSameDirection(getHeading(), Parameters.WEST))) {
					if (isTeamA) {
						stepTurn(Parameters.Direction.LEFT);
					} else stepTurn(Parameters.Direction.RIGHT);
					return;
				} else {
						rdv_point = false;
						state = State.MOVING;
					}
				return;
			}
		}
	/// TOO DECOMMENTTTTTTTTTTTTTTTT
		isShooterAround = false;
		//isShooterAround = true;

		// J'avance si au moins un allié est à moins de 500 de distance
		for (Map.Entry<String, BotState> entry : allyPos.entrySet()) {
			double distance = distance(entry.getValue().getPosition(), myPos);

			if (entry.getValue().isAlive() && distance < 700 && entry.getKey() != NBOT && entry.getKey() != SBOT) {
				//System.out.println(" shooter found ");
				isShooterAround = true;
				break;
			}
		}
		//System.out.println("isShooterAround " + isShooterAround);
		
		if (getHealth() <= 0) {
			state = State.DEAD;
			allyPos.put(whoAmI, new BotState(myPos.getX(), myPos.getY(), false));
			broadcast("DEAD " + whoAmI);
			return;
		}

		if (freeze || !isShooterAround) return;
	
		
		try {
			switch (state) {
				case MOVING :				
					myMove(true);
					break;
				case MOVING_BACK :
				    // Ici, on recule jusqu'à atteindre la cible calculée pour le recul
				    if (!hasReachedTarget(targetX, targetY, false)) {
				        myMove(false);
				    } else {
				        // Une fois la cible atteinte, on peut par exemple relancer l'évitement ou passer à un autre état
				        initiateObstacleAvoidance();
				    }
				    break;
				case TURNING_LEFT :
					turnLeft();
					break;
				case TURNING_RIGHT:
					turnRight();	
					break;
				default:
					break;	
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//=========================================ADDED=========================================

	@Override
	protected void detection () {
		// Détection des ennemis et envoi d'infos
		freeze = false;
		// Dans votre méthode principale
		for (IRadarResult o : detectRadar()) {
			double oX = myPos.getX() + o.getObjectDistance() * Math.cos(o.getObjectDirection());
			double oY = myPos.getY() + o.getObjectDistance() * Math.sin(o.getObjectDirection());
			if (allyPos.get(whoAmI).isAlive() && o.getObjectType() != IRadarResult.Types.BULLET) {
				if (state == State.MOVING_BACK) {
					for (Position p : getObstacleCorners(o, myPos.getX(), myPos.getY())) {
				        boolean obstacleInPath = isPointInTrajectory(myPos.getX(), myPos.getY(), normalize(getHeading() + Math.PI), p.getX(), p.getY());
						//System.out.println(whoAmI + " " + myPos.getX()+ " " + myPos.getY() + " || "+ p.getX() + " " + p.getY());
	
				        if (obstacleInPath) {
				            //sendLogMessage("Obstacle détecté dans la trajectoire circulaire !");
				            //System.out.println("Obstacle détecté");
				            obstacleDetected = true;
				            obstacleDirection = o.getObjectDirection();
				            initiateObstacleAvoidance();
				        } 
				    }
				} 
				else {
					for (Position p : getObstacleCorners(o, myPos.getX(), myPos.getY())) {
				        boolean obstacleInPath = isPointInTrajectory(myPos.getX(), myPos.getY(), getHeading(), p.getX(), p.getY());
						//System.out.println(whoAmI + " " + myPos.getX()+ " " + myPos.getY() + " || "+ p.getX() + " " + p.getY());
	
				        if (obstacleInPath) {
				            //sendLogMessage("Obstacle détecté dans la trajectoire circulaire !");
				            //System.out.println("Obstacle détecté");
				            obstacleDetected = true;
				            obstacleDirection = o.getObjectDirection();
				            initiateObstacleAvoidance();
				        } 
				    }
				}
			}
			switch (o.getObjectType()) {
				case OpponentMainBot:
				case OpponentSecondaryBot:
					// Transmettre la position des ennemis : ENEMY dir dist type enemyX enemyY
					//System.out.println(" Deetect ennemy TEAM AAA " + isTeamA);
					broadcast("ENEMY " + o.getObjectDirection() + " " + o.getObjectDistance() + " " + o.getObjectType() + " " + oX + " " + oY);
					//sendLogMessage("ENEMY " + o.getObjectType() + " " + enemyX + " " + enemyY);
					if (o.getObjectDistance() < BOT_RADIUS*2.5) {
						broadcast("MOVING_BACK " + whoAmI + " " + oX + " " + oY);
						freeze = true;
					}
					break;
			
				case Wreck:
					broadcast("WRECK " + o.getObjectDirection() + " " + o.getObjectDistance() + " " + o.getObjectType() + " " + oX + " " + oY);
					//sendLogMessage("ENEMY " + o.getObjectType() + " " + enemyX + " " + enemyY);
					break;
				default:
					break;
			}
	    	
	    }		
	}
		
	// Interprète les messages des alliés
	private void readMessages() {
        ArrayList<String> messages = fetchAllMessages();
        for (String msg : messages) {
            String[] parts = msg.split(" ");
            switch (parts[0]) {
	            case "POS" :
	            	double allyX = Double.parseDouble(parts[2]);
	                double allyY = Double.parseDouble(parts[3]);
	            	allyPos.put(parts[1], new BotState(allyX, allyY, true));
	            	break;
            }
        }
    }


}

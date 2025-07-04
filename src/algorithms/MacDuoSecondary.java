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

class Ennemy {
	double x, y;
	double previousX, previousY;
	double prevPreviousX, prevPreviousY;
	double distance, direction, previousDirection;
	Types type;
	double speedX, speedY;
	boolean hasMovedTwice;
	double predictedX, predictedY;

	public Ennemy(double x, double y, double distance, double direction, Types type) {
		this.x = x;
		this.y = y;
		this.distance = distance;
		this.direction = direction;
		this.previousDirection = direction;
		this.previousX = x;
		this.previousY = y;
		this.prevPreviousX = x;
		this.prevPreviousY = y;
		this.type = type;
		this.speedX = 0;
		this.speedY = 0;
		this.hasMovedTwice = false;
		this.predictedX = x;
		this.predictedY = y;
	}

	public void updatePosition(double newX, double newY, double newDistance, double newDirection) {
		this.prevPreviousX = this.previousX;
		this.prevPreviousY = this.previousY;
		this.previousX = this.x;
		this.previousY = this.y;
		this.previousDirection = this.direction;

		this.x = newX;
		this.y = newY;
		this.distance = newDistance;
		this.direction = newDirection;

		if (hasMovedTwice) {
			double dx = x - previousX;
			double dy = y - previousY;
			this.speedX = dx; // Vitesse actuelle
			this.speedY = dy;
		} else if (x != previousX || y != previousY) {
			hasMovedTwice = true;
		}
	}

	public void predictPosition(double bulletTravelTime) {
		if (!hasMovedTwice) {
			this.predictedX = x;
			this.predictedY = y;
		} else {
			// Calcul de l'accélération ou détection d'oscillation
			double prevDx = previousX - prevPreviousX;
			double prevDy = previousY - prevPreviousY;
			double currentDx = x - previousX;
			double currentDy = y - previousY;

			// Vérifie si le mouvement change de direction (oscillation)
			boolean isOscillatingX = (prevDx * currentDx < 0); // Changement de signe en X
			boolean isOscillatingY = (prevDy * currentDy < 0); // Changement de signe en Y

			if (isOscillatingX || isOscillatingY) {
				// Si oscillation détectée, limiter la prédiction à une position moyenne ou actuelle
				this.predictedX = (x + previousX) / 2; // Position moyenne comme approximation
				this.predictedY = (y + previousY) / 2;
			} else {
				// Mouvement linéaire : extrapolation basée sur la vitesse actuelle
				this.predictedX = x + speedX * bulletTravelTime;
				this.predictedY = y + speedY * bulletTravelTime;
			}
		}
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getPredictedX() {
		return predictedX;
	}

	public double getPredictedY() {
		return predictedY;
	}

	public Types getType() {
		return type;
	}
}

class BotState {
	private Position position = new Position(0, 0);
	private boolean isAlive = true;
    String whoAmI;
    double getHeading;

	public BotState() {}
	public BotState(double x, double y, boolean alive, String whoAmI, double getHeading) {
		position.setX(x);
		position.setY(y);
		isAlive = alive;
        this.whoAmI = whoAmI;
        this.getHeading = getHeading;
	}

	public void setPosition(double x, double y, double getHeading) {
		position.setX(x);
		position.setY(y);
		this.getHeading = getHeading;
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
	protected final double DISTANCE_SCOUT_SHOOTER = 1250;

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
	protected List<Ennemy> enemyTargets = new ArrayList<>();
	protected List<double[]> wreckPositions = new ArrayList<>();
	protected List<Ennemy> enemyPosToAvoid = new ArrayList<>();

	
	
	protected Map<String, BotState> allyPos = new HashMap<>();	// Stocker la position des alliés
	protected Map<Position, IRadarResult.Types> oppPos = new HashMap<>();	// Stocker la position des opposants
	protected Map<String, Double[]> wreckPos = new HashMap<>();	// Stocker la position des débris
	
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

	    // Calculer la distance
	    double distanceToScout = Math.sqrt(Math.pow(myPos.getX() - tX, 2) + Math.pow(myPos.getY() - tY, 2));

	    // Vérifier si on est dans la portée du scout
	    if (whoAmI==NBOT || whoAmI==SBOT || distanceToScout > 450) {

		    // Adapter la direction aux angles cardinaux et diagonaux
		    angleToTarget = getNearestAllowedDirection(angleToTarget);
		    if (!isSameDirection(getHeading(), angleToTarget)) {
		        turnTo(angleToTarget);
		    } else {
		        myMove(true);
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
	    } else {
	        stepTurn(Parameters.Direction.LEFT);
	    }
	}
	
	//=========================================BASE=========================================

	protected boolean isSameDirection(double dir1, double dir2) {
		double diff = Math.abs(normalize(dir1) - normalize(dir2));
		return diff < ANGLEPRECISION || Math.abs(diff - 2 * Math.PI) < ANGLEPRECISION;
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
		broadcast("POS "+whoAmI+" "+myPos.getX()+" "+myPos.getY()+" "+getHeading());
	}
	
	protected void turnLeft() {
		double avoidance = 0.5;
		if (!isSameDirection(getHeading(),oldAngle+(-avoidance * Math.PI))) {
			stepTurn(Parameters.Direction.LEFT);
	    } else {
	        state = State.MOVING;
	        myMove(true);
	    }				
	}
	
	protected void turnRight() {
		double avoidance = 0.5;
		if (!isSameDirection(getHeading(),oldAngle+(avoidance * Math.PI))) {
            stepTurn(Parameters.Direction.RIGHT);
	    } else {
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
		double pathLength = BOT_RADIUS*2; // Longueur de la trajectoire
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
		double avoidance = 0.5;


		for (IRadarResult o : detectRadar()) {
			myPos.getX();
			o.getObjectDistance();
			Math.cos(o.getObjectDirection());
			myPos.getY();
			o.getObjectDistance();
			Math.sin(o.getObjectDirection());
			if (allyPos.get(whoAmI).isAlive() && o.getObjectType() != Types.BULLET) {
				for (Position p : getObstacleCorners(o, myPos.getX(), myPos.getY())) {
					if (!obstacleInPathRight) {
						obstacleInPathRight = isPointInTrajectory(myPos.getX(), myPos.getY(), (getHeading() + avoidance * Math.PI), p.getX(), p.getY());
					}
					if (!obstacleInPathLeft) {
						if(whoAmI==SBOT)
					    obstacleInPathLeft = isPointInTrajectory(myPos.getX(), myPos.getY(), (getHeading() - avoidance * Math.PI), p.getX(), p.getY());
					}
			    }
			}
		}

		if (!obstacleInPathRight) {
			state = State.TURNING_RIGHT;
			targetX = myPos.getX() + Math.cos(getHeading()+avoidance*Math.PI) *50;
			targetY= myPos.getY() + Math.sin(getHeading()+avoidance*Math.PI) *50;

			return;
		}
		if (!obstacleInPathLeft) {
			state = State.TURNING_LEFT;
			targetX = myPos.getX() + Math.cos(getHeading()-avoidance*Math.PI) *50;
			targetY= myPos.getY() + Math.sin(getHeading()-avoidance*Math.PI) *50;
			return;
		}
		state = State.MOVING_BACK;
		targetX = myPos.getX() - Math.cos(getHeading()) *50;
		targetY= myPos.getY() - Math.sin(getHeading()) *50;
		return;
	}
	
	protected void myMove(boolean forward) {
		double speed = (whoAmI == NBOT || whoAmI == SBOT) ? Parameters.teamASecondaryBotSpeed : Parameters.teamAMainBotSpeed;

		if (forward) {
			double myPredictedX = myPos.getX() + Math.cos(getHeading()) * speed;
			double myPredictedY = myPos.getY() + Math.sin(getHeading()) * speed;
			
			if (whoAmI == NBOT || whoAmI == SBOT) {
				if (myPredictedX > 150 && myPredictedX < 2850 && myPredictedY > 150 && myPredictedY < 1850){
					move(); 
					myPos.setX(myPredictedX);
					myPos.setY(myPredictedY);
					sendMyPosition();
					return;
				}
			} else {
				if (myPredictedX > 100 && myPredictedX < 2900 && myPredictedY > 100 && myPredictedY < 1900){
					
					move(); 
					myPos.setX(myPredictedX);
					myPos.setY(myPredictedY);
					sendMyPosition();
					return;
				}
			}
		} else {
			double myPredictedX = myPos.getX() - Math.cos(getHeading()) * speed;
			double myPredictedY = myPos.getY() - Math.sin(getHeading()) * speed;

			if (whoAmI == NBOT || whoAmI == SBOT) {
				if (myPredictedX > 150 && myPredictedX < 2850 && myPredictedY > 150 && myPredictedY < 1850){
					moveBack(); 
					myPos.setX(myPredictedX);
					myPos.setY(myPredictedY);
					sendMyPosition();
					return;
				}
			} else {
				if (myPredictedX > 100 && myPredictedX < 2900 && myPredictedY > 100 && myPredictedY < 1900){
					moveBack(); 
					myPos.setX(myPredictedX);
					myPos.setY(myPredictedY);
					sendMyPosition();
					return;
				}
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

public class MacDuoSecondary extends MacDuoBaseBot{	
    
    
    
    // points de rdv
    //private static final double TARGET_X1 = 600; // TargetX2 - 300
    //private static final double TARGET_Y1 = 840; // TargetY2 - 660
    //private static final double TARGET_X2 = 900; 
    //private static final double TARGET_Y2 = 1500;
    
    private double rdvY; // TargetY2 - 660

    private boolean isShooterAround = false;
        
    private boolean firstTurning = true;

    //=========================================CORE=========================================	
	public MacDuoSecondary() {super();}

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

		
	    //INIT
	    rdv_point=true;
	    state = State.FIRST_RDV;
	    oldAngle = myGetHeading();

    	rdvY = (whoAmI == SBOT)? 1650 : 750;

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
		isShooterAround = false;

		// J'avance si au moins un allié est à moins de 500 de distance
		for (Map.Entry<String, BotState> entry : allyPos.entrySet()) {
			double distance = distance(entry.getValue().getPosition(), myPos);

			if (entry.getValue().isAlive() && distance < DISTANCE_SCOUT_SHOOTER && entry.getKey() != NBOT && entry.getKey() != SBOT) {
				isShooterAround = true;
				break;
			}
		}
		
		if (getHealth() <= 0) {
			state = State.DEAD;
			allyPos.put(whoAmI, new BotState(myPos.getX(), myPos.getY(), false, whoAmI, getHeading()));
			broadcast("DEAD " + whoAmI);
			return;
		}
		if (freeze|| !isShooterAround) return;
	
		
		try {
			switch (state) {
				case MOVING :
					boolean b = false;
					for (Map.Entry<String, BotState> entry : allyPos.entrySet()) {
						double distance = distance(entry.getValue().getPosition(), myPos);
						if (entry.getValue().isAlive() && distance < DISTANCE_SCOUT_SHOOTER && entry.getKey() != NBOT && entry.getKey() != SBOT) {
							b = true;
							break;
						}
					}
					if (!b){
						myMove(false);
					}else {
						myMove(true);
					}
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
				        if (obstacleInPath) {
				            obstacleDirection = o.getObjectDirection();
							freeze = true;
				            initiateObstacleAvoidance(); // STRATEGIE ANTI NOOB
				        } 
				    }
				} 
				else {
					for (Position p : getObstacleCorners(o, myPos.getX(), myPos.getY())) {
				        boolean obstacleInPath = isPointInTrajectory(myPos.getX(), myPos.getY(), getHeading(), p.getX(), p.getY());
	
				        if (obstacleInPath) {
				            obstacleDirection = o.getObjectDirection();
							freeze = true;
				            initiateObstacleAvoidance(); // STRATEGIE ANTI NOOB
				        } 
				    }
				}
			}
			switch (o.getObjectType()) {
				case OpponentMainBot:
				case OpponentSecondaryBot:
					// Transmettre la position des ennemis : ENEMY dir dist type enemyX enemyY
					broadcast("ENEMY " + o.getObjectDirection() + " " + o.getObjectDistance() + " " + o.getObjectType() + " " + oX + " " + oY);
					if (o.getObjectDistance() < BOT_RADIUS*4) {
						broadcast("MOVING_BACK " + whoAmI + " " + oX + " " + oY);
						freeze = true;
					}
					break;
			
				case Wreck:
					broadcast("WRECK " + oX + " " + oY);
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
	                double heading = Double.parseDouble(parts[4]);
	            	allyPos.put(parts[1], new BotState(allyX, allyY, true,parts[1], heading));
	            	break;
				case "DEAD" :
					BotState bot = allyPos.get(parts[1]);
					bot.setAlive(false);
                    break;
            }
        }
    }


}
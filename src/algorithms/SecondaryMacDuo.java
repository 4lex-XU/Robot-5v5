package algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import algorithms.MacDuoBaseBot.State;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
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
	protected Position myPos;
	protected boolean freeze;
    protected boolean isTeamA;
	protected boolean rdv_point;
	protected boolean turningTask = false;
	
	 //---VARIABLES---//
	protected State state;
	protected boolean isMoving;
	protected double oldAngle;
	
	
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

	protected abstract void myMove(boolean forward);
	protected abstract void detection();
	
	protected void reach_rdv_point(double tX, double tY) {
	    double angleToTarget = Math.atan2(tY - myPos.getY(), tX - myPos.getX());

	    // Calculer la distance au scout
	    double distanceToScout = Math.sqrt(Math.pow(myPos.getX() - tX, 2) + Math.pow(myPos.getY() - tY, 2));

	    // Vérifier si on est dans la portée du scout
	    if (distanceToScout <= 400) {
	        rdv_point = false;
	        state = State.MOVING;
	        return;
	    }

	    // Adapter la direction aux angles cardinaux et diagonaux
	    angleToTarget = getNearestAllowedDirection(angleToTarget);

	    if (!isSameDirection(getHeading(), angleToTarget)) {
	        turnTo(angleToTarget);
	    } else {
	        myMove(true);
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
		return diff < ANGLEPRECISION || Math.abs(diff - 2 * Math.PI) < ANGLEPRECISION;
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
		
		if (!isSameDirection(getHeading(),oldAngle+Parameters.LEFTTURNFULLANGLE)) {
			stepTurn(Parameters.Direction.LEFT);
	    } else {
	    	turningTask = false;
	    	System.out.println("trying to move");
	        state = State.MOVING;
	        myMove(true);
	    }				
	}
	
	protected void turnRight() {
		if (!isSameDirection(getHeading(),oldAngle+Parameters.RIGHTTURNFULLANGLE)) {
            stepTurn(Parameters.Direction.RIGHT);
	    } else {
	    	turningTask = false;
	    	System.out.println("trying to move");
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
	private double getNearestAllowedDirection(double angle) {
	    double[] allowedAngles = {
	        0,                      // Est (0°)
	        Math.PI / 4,            // Nord-Est (45°)
	        Math.PI / 2,            // Nord (90°)
	        3 * Math.PI / 4,        // Nord-Ouest (135°)
	        Math.PI,                // Ouest (180°)
	        -3 * Math.PI / 4,       // Sud-Ouest (-135°)
	        -Math.PI / 2,           // Sud (-90°)
	        -Math.PI / 4            // Sud-Est (-45°)
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
}

//=================================================================================
//====================================SECONDARY====================================
//=================================================================================

public class SecondaryMacDuo extends MacDuoBaseBot{	
    
    private int count;
    
    // points de rdv
    private static final double TARGET_X1 = 600; // TargetX2 - 300
    private static final double TARGET_Y1 = 840; // TargetY2 - 660
    private static final double TARGET_X2 = 900; 
    private static final double TARGET_Y2 = 1500;
    
    private boolean isShooterAround = false;
    
    private double targetX, targetY; 
    
    private boolean obstacleDetected = false;
    private double obstacleDirection = 0;
    private double avoidanceAngle = Math.PI/2;
    private int avoidanceTimer = 0;
    private static final int AVOIDANCE_DURATION = 10;
    private static final double OBSTACLE_AVOIDANCE_DISTANCE = 150;
    private Parameters.Direction turnedDirection;
    
	//=========================================CORE=========================================	
	public SecondaryMacDuo() {super();}

	@Override
	public void activate() {
		isTeamA = isHeading(Parameters.EAST);

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

	    sendLogMessage(whoAmI+" activé, position : " + myPos.getX() + "," + myPos.getY());
		
	    //INIT
	    isMoving=true;
	    rdv_point=true;
	    state = State.MOVING;
	    oldAngle = myGetHeading();
	    targetX = (whoAmI == SBOT) ? TARGET_X2 : TARGET_X1;
	    targetY = (whoAmI == SBOT) ? TARGET_Y2 : TARGET_Y1;
	}

	@Override
	public void step() {
		
		//DEBUG MESSAGE
        boolean debug = true;
        if (debug && whoAmI == NBOT) {
        	sendLogMessage("#NBOT *thinks* (x,y)= ("+(int)myPos.getX()+", "+(int)myPos.getY()+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"°. #State= "+state);
        }
        if (debug && whoAmI == SBOT) {
        	sendLogMessage("#SBOT *thinks* (x,y)= ("+(int)myPos.getX()+", "+(int)myPos.getY()+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"°. #State= "+state);
        }
		
		detection();
		readMessages();
		
		isShooterAround = false;

		// J'avance si au moins un allié est à moins de 500 de distance
		for (Map.Entry<String, BotState> entry : allyPos.entrySet()) {
			double distance = distance(entry.getValue().getPosition(), myPos);

			if (entry.getValue().isAlive() && distance < 500 && entry.getKey() != NBOT && entry.getKey() != SBOT) {
				isShooterAround = true;
				break;
			}
		}

		if (freeze || !isShooterAround) return;
		
		if (getHealth() <= 0) {
			state = State.DEAD;
			allyPos.put(whoAmI, new BotState(myPos.getX(), myPos.getY(), false));
		}
		
		try {
			switch (state) {
				case FIRST_RDV:
					if (rdv_point) {
						//reach_rdv_point(targetX, targetY);
					}
					break;
				case MOVING :				
					myMove(true);
					break;
				case MOVING_BACK :
					myMove(false);
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
	    for (IRadarResult o : detectRadar()) {
	    	if (o.getObjectType() == IRadarResult.Types.OpponentMainBot || o.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
	            // Transmettre la position des ennemis : ENEMY dir dist type enemyX enemyY
	            double enemyX=myPos.getX()+o.getObjectDistance()*Math.cos(o.getObjectDirection());
	            double enemyY=myPos.getY()+o.getObjectDistance()*Math.sin(o.getObjectDirection());
	            broadcast("ENEMY " + o.getObjectDirection() + " " + o.getObjectDistance() + " " + o.getObjectType() + " " + enemyX + " " + enemyY);
	            sendLogMessage("ENEMY " + o.getObjectType() + " " + enemyX + " " + enemyY);
            	if (o.getObjectDistance() < 300) {
    	            broadcast("MOVING_BACK " + whoAmI + " " + enemyX + " " + enemyY);
    	            freeze = true;
            	}
	          
	        }
	    	if (o.getObjectType() == IRadarResult.Types.Wreck) {
	            double enemyX=myPos.getX()+o.getObjectDistance()*Math.cos(o.getObjectDirection());
	            double enemyY=myPos.getY()+o.getObjectDistance()*Math.sin(o.getObjectDirection());
	            broadcast("WRECK " + o.getObjectDirection() + " " + o.getObjectDistance() + " " + o.getObjectType() + " " + enemyX + " " + enemyY);
	            //sendLogMessage("ENEMY " + o.getObjectType() + " " + enemyX + " " + enemyY);
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
	            	if (!turningTask) state = State.MOVING;
	            	double targetX = Double.parseDouble(parts[2]);
	                double targetY = Double.parseDouble(parts[3]);
	            	allyPos.put(parts[1], new BotState(targetX, targetY, true));
	            	double distance = Math.sqrt(Math.pow(targetX - myPos.getX(), 2) + Math.pow(targetY - myPos.getY(), 2));
	            	break;
            }
        }
    }

	protected void myMove(boolean forward) {
		if (forward) {
			if(detectFront().getObjectType() == IFrontSensorResult.Types.NOTHING) {
				double myPredictedX = myPos.getX() + Math.cos(getHeading()) * Parameters.teamASecondaryBotSpeed;
				double myPredictedY = myPos.getY() + Math.sin(getHeading()) * Parameters.teamASecondaryBotSpeed;

				// évite de se bloquer dans les murs
				if(myPredictedX > 100 && myPredictedX < 2900 && myPredictedY > 100 && myPredictedY < 1900 ) {
					move(); 
					myPos.setX(myPredictedX);
					myPos.setY(myPredictedY);
		    		sendMyPosition();
		    		return;
				}
			}
		} else {
			double myPredictedX = myPos.getX() - Math.cos(getHeading()) * Parameters.teamASecondaryBotSpeed;
			double myPredictedY = myPos.getY() - Math.sin(getHeading()) * Parameters.teamASecondaryBotSpeed;

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
	
	private void initiateObstacleAvoidance() {
        // Determine which way to turn based on obstacle direction
        double relativeAngle = normalize(obstacleDirection - getHeading());
        if (turnedDirection != null) {
	        if (turnedDirection == Parameters.Direction.RIGHT) {
	        	state = State.TURNING_RIGHT;
	        }
	        else {
	        	 state = State.TURNING_LEFT;
	        }
        }
        else if (relativeAngle > 0 && relativeAngle < Math.PI) {
            // Obstacle is on the right, turn left
        	turnedDirection = Parameters.Direction.RIGHT;
        	state = State.TURNING_RIGHT;
            //gMessage("Avoiding obstacle by turning right");
        } else {
            // Obstacle is on the left, turn right
        	turnedDirection = Parameters.Direction.LEFT;
            state = State.TURNING_LEFT;
            //sendLogMessage("Avoiding obstacle by turning left");
        }
        
        oldAngle = myGetHeading();
        turningTask = true;
        avoidanceTimer = AVOIDANCE_DURATION;
    }
}

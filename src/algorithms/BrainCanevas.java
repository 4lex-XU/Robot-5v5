/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/BrainCanevas.java 2014-10-19 buixuan.
 * ******************************************************/
package algorithms;

import robotsimulator.Brain;

import java.util.HashMap;
import java.util.Map;

import algorithms.MacDuoBaseBot.State;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.Parameters;

public class BrainCanevas extends MacDuoBaseBot {

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

  public BrainCanevas() { super(); }

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

    //myMove(true);
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
	}

  @Override
  protected void detection() {

    for(IRadarResult o: detectRadar()) {
      
      double oX=myPos.getX()+o.getObjectDistance()*Math.cos(o.getObjectDirection());
			double oY=myPos.getY()+o.getObjectDistance()*Math.sin(o.getObjectDirection());

			double objectRadius = o.getObjectRadius(); // Rayon de l'objet détecté
			double distanceEffective = o.getObjectDistance() - (BOT_RADIUS + objectRadius);

			// Vérifie si l'objet est directement devant
			boolean isObstacleAhead = Math.abs(normalize(o.getObjectDirection() - getHeading())) < 0.45;
			System.out.println("Obstacle ahead : " + isObstacleAhead);
			System.out.println("Obstacle ahead : " + distanceEffective);

			if (isObstacleAhead && distanceEffective < 150) {  // Seuil de sécurité de 100 mm
				System.out.println("Obstacle détecté devant ! Initiating avoidance.");
				System.out.println(o.getObjectDistance());
				sendLogMessage("Obstacle détecté devant ! Initiating avoidance.");
				obstacleDirection = o.getObjectDirection();
				initiateObstacleAvoidance(); // Lance la manœuvre d'évitement
			}
    } 

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
  }
}

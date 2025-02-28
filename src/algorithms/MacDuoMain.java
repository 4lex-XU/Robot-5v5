package algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import characteristics.IRadarResult;
import characteristics.IFrontSensorResult.Types;
import characteristics.Parameters;
import robotsimulator.Brain;

public class MacDuoMain extends Brain {
    private static final double ANGLEPRECISION = 0.1;
    
    // les ids des shooters
    private static final int MAIN1 = 1;
    private static final int MAIN2 = 2;
    private static final int MAIN3 = 3;
    // les states
    private static final int RDV_POINT = 1;
    private static final int MOVING = 2;
    private static final int WAITING = 3;
    private static final int TURNING = 4;
    private static final int FIGHTING = 5;
    private static final int CHASING = 6;
    
    //---VARIABLES---//
    private int state;
	private boolean isMoving;
    private int whoAmI;
    private double myX, myY;
    private double targetX, targetY;  
    private boolean isTeamA = false;
    private double targetHeading;
    private double enemyX, enemyY;
    private Map<String, Double[]> scouts = new HashMap<>();

    @Override
    public void activate() {
    	boolean top = false;
    	boolean bottom = false;
        for (IRadarResult o: detectRadar())
            if (isSameDirection(o.getObjectDirection(),Parameters.NORTH)) top = true;
            else if (isSameDirection(o.getObjectDirection(),Parameters.SOUTH)) bottom = true;
        
        whoAmI = MAIN3;
        if (top && bottom) whoAmI = MAIN2;
        else if (!top && bottom) whoAmI = MAIN1; 
        
        if (isHeading(Parameters.EAST)) isTeamA = true;
        
        switch(whoAmI) {
	        case MAIN1 : 
	        	myX = isTeamA ? Parameters.teamAMainBot1InitX : Parameters.teamBMainBot1InitX;
	            myY = isTeamA ? Parameters.teamAMainBot1InitY : Parameters.teamBMainBot1InitY;
	            targetX = isTeamA ? 300 : 2500;
	            targetY = 1100;
                sendLogMessage("targetX and targetY : " + targetX + ", " + targetY);
                break;
	        case MAIN2 : 
	        	myX = isTeamA ? Parameters.teamAMainBot2InitX : Parameters.teamBMainBot2InitX;
	            myY = isTeamA ? Parameters.teamAMainBot2InitY : Parameters.teamBMainBot2InitY;
	            targetX = isTeamA ? 450 : 2400;
	            targetY = 1350;
                sendLogMessage("targetX and targetY : " + targetX + ", " + targetY);
	            break;
	        case MAIN3 : 
	        	myX = isTeamA ? Parameters.teamAMainBot3InitX : Parameters.teamBMainBot3InitX;
	            myY = isTeamA ? Parameters.teamAMainBot3InitY : Parameters.teamBMainBot3InitY;
	            targetX = isTeamA ? 600 : 2300;
	            targetY = 1700;
                sendLogMessage("targetX and targetY : " + targetX + ", " + targetY);
	            break;
        }
	    isMoving=true;
	    state = RDV_POINT;
    }
    
    @Override
    public void step() {
        switch (state) {
            case RDV_POINT:
                moveToTarget();
                break;
            case WAITING:
                readMessages();
                break;
            case FIGHTING:
                handleFighting();
                break;
            case CHASING:
                handleChasing();
                break;
            case TURNING:
                turnTo(targetHeading);
                if (isSameDirection(getHeading(), targetHeading)) {
                    state = MOVING;
                }
                break;
            case MOVING:
                moveToTarget();
                break;
        }
    }

    private void readMessages() {
        ArrayList<String> messages = fetchAllMessages();
        for (String msg : messages) {
            String[] parts = msg.split(" ");
            switch (parts[0]) {
                case "ENEMY":
                	sendLogMessage("enemy message received");
                    handleEnemyMessage(parts);
                    break;
                case "SCOUT_A":
                	handleScoutMessage(parts);
                	break;
                case "SCOUT_B":
                case "SCOUT_DOWN_A":
                case "SCOUT_DOWN_B":
                    //handleScoutMessage(parts);
                    break;
            }
        }
    }

    private void handleEnemyMessage(String[] parts) {
        double directionScoutEnemy = Math.toRadians(Double.parseDouble(parts[1]));
        double distanceScoutEnemy = Double.parseDouble(parts[2]);
        double scoutX = Double.parseDouble(parts[4]);
        double scoutY = Double.parseDouble(parts[5]);

        double enemyX = scoutX + distanceScoutEnemy * Math.cos(directionScoutEnemy);
        double enemyY = scoutY + distanceScoutEnemy * Math.sin(directionScoutEnemy);

        double dx = enemyX - myX;
        double dy = enemyY - myY;
        double angleToEnemy = Math.toDegrees(Math.atan2(dy, dx)); 
        double myHeading = getHeading(); 
        double relativeAngle = normalize(angleToEnemy - myHeading);

        double distanceEnemyMe = Math.sqrt(dx * dx + dy * dy);
        sendLogMessage("distanceEnemyMe " + distanceEnemyMe);
        if (distanceEnemyMe <= 900) {
            state = FIGHTING;
        } else {
            targetX = enemyX;
            targetY = enemyY;
            state = MOVING;
        }
//        if (distanceEnemyMe <= 900) {
//            turnTo(Math.toRadians(relativeAngle)); 
//            fire(relativeAngle); 
//        } else {
//            double moveAngle = Math.toRadians(angleToEnemy);
//            targetX = myX + Math.cos(moveAngle) * (distanceEnemyMe - 500);
//            targetY = myY + Math.sin(moveAngle) * (distanceEnemyMe - 500);
//            moveToTarget();
//        }
    }
    
    private void handleFighting() {
        double dx = enemyX - myX;
        double dy = enemyY - myY;
        double angleToEnemy = Math.toDegrees(Math.atan2(dy, dx)); 

        if (!isSameDirection(getHeading(), angleToEnemy)) {
            targetHeading = angleToEnemy;
            state = TURNING;
        } else {
            tryToFire(angleToEnemy);
            state = WAITING;
        }
    }
    
    private void handleScoutDownMessage(String[] parts) {
//        // Vérifier si les deux scouts sont morts
//        if (scouts.size() >= 2) {
//            state = CHASING;
//        }
    }
    
    private void handleScoutMessage(String[] parts) {
        String scoutType = parts[0];
        double targetX = Double.parseDouble(parts[1]);
        double targetY = Double.parseDouble(parts[2]);
        scouts.put(scoutType, new Double[]{targetX, targetY});
	    double distance = Math.sqrt(Math.pow(targetX - myX, 2) + Math.pow(targetY - myY, 2));
	    if(distance > 200) moveToTarget(); 
    }
    
    private void handleChasing() {
        // Implémenter le comportement de poursuite
        // Par exemple, se déplacer de manière aléatoire ou suivre une stratégie spécifique
        targetX = myX + (Math.random() * 100 - 50);
        targetY = myY + (Math.random() * 100 - 50);
        state = MOVING;
    }
    
    private void tryToFire(double preciseDirection) {
//        if (detectFront().getObjectType().equals(Types.TeamMainBot) || detectFront().getObjectType().equals(Types.TeamSecondaryBot)) {
//            return;
//        }
//        else {
            fire(preciseDirection);
            sendLogMessage("Tir déclenché dans la direction : " + Math.toDegrees(preciseDirection));
        //}
    }
    
    private void moveToTarget() {
        double dx = targetX - myX;
        double dy = targetY - myY;
        double angleToTarget = Math.atan2(dy, dx);

        if (!isSameDirection(getHeading(), angleToTarget)) {
//        	state=TURNING;
//        	targetHeading = angleToTarget;
            turnTo(angleToTarget);
        } else {
            move();
            myX += Math.cos(getHeading()) * Parameters.teamAMainBotSpeed;
            myY += Math.sin(getHeading()) * Parameters.teamAMainBotSpeed;
        }

	    double distance = Math.sqrt(Math.pow(targetX - myX, 2) + Math.pow(targetY - myY, 2));
	    if (distance < 5) {
	        broadcast("READY " + whoAmI);
	        state = WAITING;
	    }
    }
    
    private boolean isSameDirection(double dir1, double dir2){
        return Math.abs(normalize(dir1)-normalize(dir2))<ANGLEPRECISION;
      }
    
    private double normalize(double dir){
        double res=dir;
        while (res<0) res+=2*Math.PI;
        while (res>=2*Math.PI) res-=2*Math.PI;
        return res;
      }
    
    protected boolean isHeading(double dir){
        return Math.abs(Math.sin(getHeading()-dir))<ANGLEPRECISION;
    }
    
    protected void turnTo(double targetAngle) {
        double currentAngle = getHeading();
        double diff = normalize(targetAngle - currentAngle);

        if (diff > Math.PI) {
            diff -= 2 * Math.PI;
        } else if (diff < -Math.PI) {
            diff += 2 * Math.PI;
        }

        if (Math.abs(diff) > ANGLEPRECISION) {
            if (diff > 0) {
                stepTurn(Parameters.Direction.RIGHT);
            } else {
                stepTurn(Parameters.Direction.LEFT);
            }
        }
    }

}

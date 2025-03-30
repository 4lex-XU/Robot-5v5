package algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import characteristics.IRadarResult;
import characteristics.IRadarResult.Types;
import characteristics.Parameters;

public class MacDuoMain extends MacDuoBaseBot {
    private static final double BOT_RADIUS = 50; // Rayon du robot, comme dans BrainCanevas
    private static final double BULLET_RADIUS = Parameters.bulletRadius; // Rayon de la balle
    private static final double BOT_BULLET_RADIUS = BOT_RADIUS + BULLET_RADIUS;

    private Ennemy lastTarget = null;

    // Variables existantes
    private double rdvX = 0.0;
    private double rdvY = 0.0;
    private boolean avoidingEnnemy = false;
    private Ennemy target;
    private int fireStrike = 0;
    private static final int MAX_FIRESTRIKE = 100;

    public MacDuoMain() {
        super();
    }

    @Override
    public void activate() {
        isTeamA = (getHeading() == Parameters.EAST);

        boolean top = false;
        boolean bottom = false;
        for (IRadarResult o : detectRadar()) {
            if (isSameDirection(o.getObjectDirection(), Parameters.NORTH)) top = true;
            else if (isSameDirection(o.getObjectDirection(), Parameters.SOUTH)) bottom = true;
        }

        whoAmI = MAIN3;
        if (top && bottom) whoAmI = MAIN2;
        else if (!top && bottom) whoAmI = MAIN1;

        switch (whoAmI) {
            case MAIN1:
                myPos = new Position((isTeamA ? Parameters.teamAMainBot1InitX : Parameters.teamBMainBot1InitX),
                        (isTeamA ? Parameters.teamAMainBot1InitY : Parameters.teamBMainBot1InitY));
                break;
            case MAIN2:
                myPos = new Position((isTeamA ? Parameters.teamAMainBot2InitX : Parameters.teamBMainBot2InitX),
                        (isTeamA ? Parameters.teamAMainBot2InitY : Parameters.teamBMainBot2InitY));
                break;
            case MAIN3:
                myPos = new Position((isTeamA ? Parameters.teamAMainBot3InitX : Parameters.teamBMainBot3InitX),
                        (isTeamA ? Parameters.teamAMainBot3InitY : Parameters.teamBMainBot3InitY));
                break;
        }
        state = State.FIRST_RDV;
        oldAngle = myGetHeading();
    }

    @Override
    public void step() {
        // Debug messages
        boolean debug = true;
        

        detection();
        readMessages();
        if (getHealth() <= 0) {
            state = State.DEAD;
            allyPos.put(whoAmI, new BotState(myPos.getX(), myPos.getY(), false, whoAmI, getHeading()));
            return;
        }

        target = chooseTarget();
        if (target != null) {
            state = State.FIRE;
            if (fireStrike == 0) {
                lastTarget = target;
            }
        }

		if (debug && whoAmI == MAIN1) {
            sendLogMessage("#MAIN1 *thinks* (x,y)= ("+(int)myPos.getX()+", "+(int)myPos.getY()+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"°. #State= "+state);
        }
        if (debug && whoAmI == MAIN2) {
            sendLogMessage("#MAIN2 *thinks* (x,y)= ("+(int)myPos.getX()+", "+(int)myPos.getY()+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"°. #State= "+state);
        }
        if (debug && whoAmI == MAIN3) {
            sendLogMessage("#MAIN3 *thinks* (x,y)= ("+(int)myPos.getX()+", "+(int)myPos.getY()+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"°. #State= "+state);
        }

        try {
            switch (state) {
                case FIRE:
                    handleFire(target);
                    break;
                case FIRST_RDV:
                    if (rdvX != 0.0 && rdvY != 0.0) {
                        reach_rdv_point(rdvX, rdvY);
                    }
                    break;
                case MOVING:
                    boolean following = (allyPos.get(SBOT).isAlive() || allyPos.get(NBOT).isAlive());
                    if (following) {
                        if (!isShooterAvoiding) {
                            reach_rdv_point(rdvX, rdvY);
                        } else {
                            if (!hasReachedTarget(targetX, targetY, true)) {
                                myMove(true);
                            } else {
                                isShooterAvoiding = false;
                            }
                        }
                    } else {
                        myMove(true);
                    }
                    break;
                case MOVING_BACK:
                    if (!hasReachedTarget(targetX, targetY, false)) {
                        myMove(false);
                    } else {
                        initiateObstacleAvoidance();
                    }
                    break;
                case TURNING_LEFT:
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

    // Méthodes existantes inchangées
    protected void detection() {
        boolean enemyDetected = false;

        for (IRadarResult o : detectRadar()) {
            double oX = myPos.getX() + o.getObjectDistance() * Math.cos(o.getObjectDirection());
            double oY = myPos.getY() + o.getObjectDistance() * Math.sin(o.getObjectDirection());
            if (allyPos.get(whoAmI).isAlive() && o.getObjectType() != IRadarResult.Types.BULLET) {
                if (state == State.MOVING_BACK) {
                    for (Position p : getObstacleCorners(o, myPos.getX(), myPos.getY())) {
                        boolean obstacleInPath = isPointInTrajectory(myPos.getX(), myPos.getY(), normalize(getHeading() + Math.PI), p.getX(), p.getY());
                        if (obstacleInPath) {
                            obstacleDirection = o.getObjectDirection();
                            initiateObstacleAvoidance();
                        }
                    }
                } else {
                    for (Position p : getObstacleCorners(o, myPos.getX(), myPos.getY())) {
                        boolean obstacleInPath = isPointInTrajectory(myPos.getX(), myPos.getY(), getHeading(), p.getX(), p.getY());
                        if (obstacleInPath) {
                            obstacleDirection = o.getObjectDirection();
                            initiateObstacleAvoidance();
                        }
                    }
                }
            }
            if (o.getObjectType() == IRadarResult.Types.OpponentMainBot || o.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
                broadcast("ENEMY " + o.getObjectDirection() + " " + o.getObjectDistance() + " " + o.getObjectType() + " " + oX + " " + oY);
                addOrUpdateEnemy(oX, oY, o.getObjectDistance(), o.getObjectDirection(), true, o.getObjectType());
                if (!enemyDetected) {
                    enemyDetected = true;
                }
            }
            if (o.getObjectType() == IRadarResult.Types.Wreck) {
                broadcast("WRECK " + oX + " " + oY);
                handleWreckMessage(new String[]{"WRECK", String.valueOf(oX), String.valueOf(oY)});
            }
        }
        if (!enemyDetected && state != State.FIRE && !isShooterAvoiding) {
            state = State.MOVING;
        }
    }

    private void readMessages() {
        ArrayList<String> messages = fetchAllMessages();
        ArrayList<String> ennemyMessages = new ArrayList<String>();

        for (String msg : messages) {
            String[] parts = msg.split(" ");
            switch (parts[0]) {
                case "ENEMY":
                    ennemyMessages.add(msg);
                    break;
                case "WRECK":
                    handleWreckMessage(parts);
                    break;
                case "POS":
                    handlePosMessage(parts);
                    break;
                case "DEAD":
                    BotState bot = allyPos.get(parts[1]);
					bot.setAlive(false);
                    break;
            }
        }

        for (String msg : ennemyMessages) {
            String[] parts = msg.split(" ");
            handleEnemyMessage(parts);
        }
    }

    private void handlePosMessage(String[] parts) {
        double botX = Double.parseDouble(parts[2]);
        double botY = Double.parseDouble(parts[3]);
        double heading = Double.parseDouble(parts[4]);
        BotState bot = allyPos.get(parts[1]);
        if (bot == null) {
            allyPos.put(parts[1], new BotState(botX, botY, true, parts[1], heading));
        } else {
            bot.setPosition(botX, botY, heading);
        }
        if (parts[1].equals("SBOT")) {
            if (state != State.FIRE) {
                rdvX = botX;
                rdvY = botY;
            }
        } else if (!allyPos.get(SBOT).isAlive() && parts[1].equals("NBOT")) {
            if (state != State.FIRE) {
                rdvX = botX;
                rdvY = botY;
            }
        }
    }

    private void handleWreckMessage(String[] parts) {
        double wreckX = Double.parseDouble(parts[1]);
        double wreckY = Double.parseDouble(parts[2]);

        boolean exists = false;
        for (double[] wreck : wreckPositions) {
            if (Math.abs(wreck[0] - wreckX) < 20 && Math.abs(wreck[1] - wreckY) < 20) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            wreckPositions.add(new double[]{wreckX, wreckY});
        }

        Ennemy detectedEnemyWreck = null;
        for (Ennemy enemy : enemyTargets) {
            if (Math.abs(enemy.x - wreckX) < 50 && Math.abs(enemy.y - wreckY) < 50) {
                detectedEnemyWreck = enemy;
                break;
            }
        }
        enemyTargets.remove(detectedEnemyWreck);
        enemyPosToAvoid.add(detectedEnemyWreck != null ? detectedEnemyWreck : new Ennemy(wreckX, wreckY, 0, 0, Types.OpponentSecondaryBot));
    }

    private void handleEnemyMessage(String[] parts) {
        double enemyX = Double.parseDouble(parts[4]);
        double enemyY = Double.parseDouble(parts[5]);
        double enemyDistance = Double.parseDouble(parts[2]);
        double enemyDirection = Double.parseDouble(parts[1]);
        Types enemyType = parts[3].contains("MainBot") ? Types.OpponentMainBot : Types.OpponentSecondaryBot;

        addOrUpdateEnemy(enemyX, enemyY, enemyDistance, enemyDirection, false, enemyType);
    }

    // Nouvelle logique de visée intégrée depuis BrainCanevas
    private void handleFire(Ennemy target) {
        if (target != null) {
            if (avoidingEnnemy) {
                if (!isRoughlySameDirection(target.direction, getHeading())) {
                    turnTo(target.direction);
					avoidingEnnemy = false;
                    return;
                }
                if (distance(new Position(target.x, target.y), myPos) < 980) {
                    for (IRadarResult o : detectRadar()) {
                        if (allyPos.get(whoAmI).isAlive() && o.getObjectType() != IRadarResult.Types.BULLET) {
                            for (Position p : getObstacleCorners(o, myPos.getX(), myPos.getY())) {
                                boolean obstacleInPath = isPointInTrajectory(myPos.getX(), myPos.getY(), normalize(getHeading() + Math.PI), p.getX(), p.getY());
                                if (obstacleInPath) {
                                    initiateObstacleAvoidance();
                					avoidingEnnemy = false;
                                    return;
                                }
                            }
                            
                        }
                    }
                    myMove(false);
                } else {
                    for (IRadarResult o : detectRadar()) {
                        if (allyPos.get(whoAmI).isAlive() && o.getObjectType() != IRadarResult.Types.BULLET) {
                           for (Position p : getObstacleCorners(o, myPos.getX(), myPos.getY())) {
                                boolean obstacleInPath = isPointInTrajectory(myPos.getX(), myPos.getY(), getHeading(), p.getX(), p.getY());
                                if (obstacleInPath) {
                                    initiateObstacleAvoidance();
                					avoidingEnnemy = false;
                                    return;
                                }
                               }
                            }
                        }
                    myMove(true);
                }
                avoidingEnnemy = false;
                return;
            }

            // Utiliser la logique de BrainCanevas pour viser précisément
            double firingAngle = tryFindAngle(myPos, target);
            if (firingAngle != Double.NaN) {
                fire(firingAngle);
                if (target.equals(lastTarget)) fireStrike++;
                else fireStrike = 0;
                if (fireStrike >= MAX_FIRESTRIKE) {
                    enemyTargets.remove(target);
                    fireStrike = 0;
                }
                avoidingEnnemy = true;
            } else {
                state = State.MOVING; // Pas de tir possible, retour à MOVING
            }
        } else {
            state = State.MOVING;
        }
    }

    private double tryFindAngle(Position from, Ennemy to) {
        double distance = distance(new Position(to.x, to.y), from);
        double time = distance / Parameters.bulletVelocity;

        to.predictPosition(time);
        double futureX = to.getPredictedX();
        double futureY = to.getPredictedY();
        new Position(futureX, futureY);

        double centerAngle = Math.atan2(futureY - from.getY(), futureX - from.getX());
        double vectorNorm = distance(new Position(futureX, futureY), from);

        double maxDeviation = Math.atan((BOT_BULLET_RADIUS - 1) / Math.max(vectorNorm, 1.0));

        double[] angles = {centerAngle, centerAngle - maxDeviation, centerAngle + maxDeviation};
        for (double angle : angles) {
            Position firingEnd = new Position(
                from.getX() + Math.cos(angle) * Parameters.bulletRange,
                from.getY() + Math.sin(angle) * Parameters.bulletRange
            );
            if (isFiringLineSafe(firingEnd)) {
                return angle;
            }
        }
        return Double.NaN; 
    }

    private boolean isFiringLineSafe(Position end) {
        for (BotState ally : allyPos.values()) {
            if (ally.getPosition().getX() == myPos.getX() && ally.getPosition().getY() == myPos.getY()) {
                continue;
            }
            if (isObstacleOnMyFire(ally.getPosition(), end, BOT_RADIUS)) {
                return false;
            }
        }
        for (double[] wreck : wreckPositions) {
            Position wreckCenter = new Position(wreck[0], wreck[1]);
            if (isObstacleOnMyFire(wreckCenter, end, BOT_RADIUS)) {
                return false;
            }
        }
        return true;
    }

    private void addOrUpdateEnemy(double x, double y, double distance, double direction, boolean isMyDetection, Types type) {
        if (!isMyDetection) {
            double dx = x - myPos.getX();
            double dy = y - myPos.getY();
            distance = Math.sqrt(dx * dx + dy * dy);
            direction = Math.atan2(dy, dx);
        }

        for (Ennemy enemy : enemyTargets) {
            if (Math.abs(enemy.x - x) < 50 && Math.abs(enemy.y - y) < 50) {
                enemy.updatePosition(x, y, distance, direction);
                return;
            }
        }
        enemyTargets.add(new Ennemy(x, y, distance, direction, type));
    }

    private boolean isObstacleOnMyFire(Position obstacleCenter, Position target, double obstacleRadius) {
        double startX = myPos.getX();
        double startY = myPos.getY();
        double endX = target.getX();
        double endY = target.getY();
        double allyX = obstacleCenter.getX();
        double allyY = obstacleCenter.getY();

        double deltaX = endX - startX;
        double deltaY = endY - startY;
        double lineLengthSquared = deltaX * deltaX + deltaY * deltaY;
        double lineLength = Math.sqrt(lineLengthSquared);

        double EPSILON = 1e-6;
        if (lineLength < EPSILON) {
            return false;
        }

        double dirX = deltaX / lineLength;
        double dirY = deltaY / lineLength;

        double vecX = allyX - startX;
        double vecY = allyY - startY;

        double projection = vecX * dirX + vecY * dirY;

        if (projection < -EPSILON || projection > lineLength + EPSILON) {
            return false;
        }

        double perpendicularDistance = Math.abs(vecX * dirY - vecY * dirX);

        double effectiveRadius = obstacleRadius + Parameters.bulletRadius + EPSILON;

        if (perpendicularDistance < effectiveRadius) {
            double distanceToStart = Math.sqrt(vecX * vecX + vecY * vecY);
            double distanceToEnd = Math.sqrt((allyX - endX) * (allyX - endX) + (allyY - endY) * (allyY - endY));
            if (distanceToStart < obstacleRadius || distanceToEnd < obstacleRadius) {
                return true;
            }
            return true;
        }
        return false;
    }

    private double calculateBulletTravelTime(double targetX, double targetY) {
        double dx = targetX - myPos.getX();
        double dy = targetY - myPos.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance / Parameters.bulletVelocity;
    }

    private Ennemy chooseTarget() {
        Collections.sort(enemyTargets, (e1, e2) -> Double.compare(e1.distance, e2.distance));
        for (Ennemy enemy : enemyTargets) {
            double bulletTravelTime = calculateBulletTravelTime(enemy.x, enemy.y);
            enemy.predictPosition(bulletTravelTime);
            Position predictedTarget = new Position(enemy.getPredictedX(), enemy.getPredictedY());

            boolean obstacleInTheWay = false;

            for (BotState ally : allyPos.values()) {
                if (ally.getPosition().getX() == myPos.getX() && ally.getPosition().getY() == myPos.getY()) {
                    continue;
                }
                Position predictedAllyPos = new Position(ally.getPosition().getX(), ally.getPosition().getY());
                if (ally.whoAmI == NBOT || ally.whoAmI == SBOT) {
                    for (Map.Entry<String, BotState> entry : allyPos.entrySet()) {
                        double distance = distance(entry.getValue().getPosition(), myPos);
                        if (entry.getValue().isAlive() && distance < DISTANCE_SCOUT_SHOOTER && entry.getKey() != NBOT && entry.getKey() != SBOT) {
                            predictedAllyPos = new Position(ally.getPosition().getX() + Math.cos(getHeading()) * 3, ally.getPosition().getY() + Math.sin(getHeading()) * 3);
                            break;
                        }
                    }
                } else {
                    predictedAllyPos = new Position(ally.getPosition().getX() + Math.cos(getHeading()), ally.getPosition().getY() + Math.sin(getHeading()));
                }
                if (isObstacleOnMyFire(predictedAllyPos, predictedTarget, BOT_RADIUS)) {
                    obstacleInTheWay = true;
                    break;
                }
            }

            /*for (double[] wreck : wreckPositions) {
                Position wreckCenter = new Position(wreck[0], wreck[1]);
                if (isObstacleOnMyFire(wreckCenter, predictedTarget, BOT_RADIUS)) {
                    obstacleInTheWay = true;
                    break;
                }
            }*/

            if (!obstacleInTheWay) {
                return enemy;
            }
        }
        return null;
    }
}
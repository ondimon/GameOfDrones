package com.company;

import java.util.*;
import java.io.*;
import java.math.*;
import java.awt.Point;

class Player {

    Team[] teams; // all the team of drones. Array index = team's ID
    int myTeamIdx; // index of my team in the array of teams
    Zone[] zones; // all game zones
    String[] commands;
    HashMap<Drone,Boolean> free_dron = new HashMap<Drone, Boolean>();
    HashMap<Zone,Boolean> free_zone = new HashMap<Zone, Boolean>();
    Phase phase;

    enum Phase {Capture,Attack}

    static class Utils {

        static void pLog1(HashMap<Unit,Boolean> map){
            System.err.println("------"+map.keySet().toArray()[0].getClass());
            for (Map.Entry<Unit,Boolean> key_value : map.entrySet()) {
                System.err.println(""+key_value.getKey().id + " " +key_value.getValue());
            }
        }

        static void pLog2(HashMap<Unit,Unit> map){
            System.err.println("------"+map.keySet().toArray()[0].getClass()+"-"+map.values().toArray()[0].getClass());
            for (Map.Entry<Unit,Unit> key_value : map.entrySet()) {
                System.err.println(""+key_value.getKey().id + " " +key_value.getValue().id);
            }
        }

        static int distance(Point depart, Point arrivee) {
            return (int) Math.sqrt( Math.pow(arrivee.x - depart.x, 2) + Math.pow(arrivee.y - depart.y, 2) );
        }

        static Point getCenter(List<Point> points) {
            int x =0,y=0;
            for (Point point:points) {
                x += point.x;
                y += point.y;
            }
            x /= points.size();
            y /= points.size();
            return  new Point(x,y);
        }
        static HashMap<Drone,Zone> FindBestZone(List<Drone> drones, List<Zone> zones){
            HashMap<Drone,Zone> result = new HashMap<Drone,Zone>(drones.size());
            for (Drone drone:drones) {
                int min_dist = Integer.MAX_VALUE;
                Zone id_zone = null;
                for (Zone zone:zones) {
                    int dist_to_zone = distance(drone.pos,zone.pos);
                    if (dist_to_zone < min_dist) {
                        min_dist = dist_to_zone;
                        id_zone = zone;
                    }
                }
                result.put(drone,id_zone);
            }
            return result;
        }
    }

    class Unit {
        int id;
        Point pos;
    }

    class Team {
        int id;
        Drone[] drones; // position of the drones

    }

    class Zone extends Unit{
        Zone(){
            pos = new Point();
        }
        int owner = -1; // ID of the team which owns this zone, -1 otherwise
        int count_drone_owner = 0;
        int max_enemy_drones = 0;
        ArrayList<Drone> myDronesInZone = new ArrayList<Drone>();
    }

    class Drone extends Unit{
        int id_team;
        Drone(int id,int id_team) {
            this.id = id;
            this.id_team = id_team;
            this.pos = new Point();
        }
        Drone(int id,int x,int y){
            this.id = id;
            this.pos = new Point(x,y);
        }
    }

    /**
     * Compute logic here. This method is called for each game round.
     */
    void play() throws InterruptedException {

        /*
        HashMap<Unit,Unit>LogMap2;
        HashMap<Unit,Boolean>LogMap1;

        LogMap2 = new HashMap<Unit,Unit>(targets_drone);
        Utils.pLog2(LogMap2);
        LogMap1  = new HashMap<Unit,Boolean>(free_zone);
        Utils.pLog1(LogMap1);
        LogMap1  = new HashMap<Unit,Boolean>(free_dron);
        Utils.pLog1(LogMap1);
            */
        if (phase == Phase.Capture) {
            Team myTeam = teams[myTeamIdx];
            boolean allDroneinZone = true;
            for (Drone drone : myTeam.drones) {
                boolean DroneinZone = false;
                for (Zone zone : zones) {
                    if (DroneInZone(drone,zone)) {
                        DroneinZone = true;
                        break;
                    }
                }
                if (!DroneinZone) {
                    allDroneinZone = false;
                    break;

                }
            }
            if(allDroneinZone) phase = Phase.Attack;
        }

        if (phase == Phase.Capture) {
            HashMap<Drone, Zone> targets_drone = Utils.FindBestZone(getFreeDrone(), Arrays.asList(zones));
            setCommands(targets_drone, false);

            targets_drone.clear();
            targets_drone = Utils.FindBestZone(getFreeDrone(), getFreeZone());
            setCommands(targets_drone, false);

            targets_drone.clear();
            targets_drone = Utils.FindBestZone(getFreeDrone(), Arrays.asList(zones));
            setCommands(targets_drone, true);
        }else if (phase == Phase.Attack) {
            //Обороняем захваченое
            ArrayList<Zone> myZones = getMyZones();
            for(Zone zone : myZones) {
                if (zone.myDronesInZone.size() == 0) continue;
                for (int i=0;i<zone.max_enemy_drones;i++) {
                    if (i >= zone.myDronesInZone.size()) break;
                    Drone drone = zone.myDronesInZone.get(i);
                    setCommands(drone,zone);
                }
            }
            //Захватываем нейтральные
            SpreadDronesOnZones(getFreeDrone(),getNeutralZones());
            SpreadDronesOnZones(getFreeDrone(),getEnemyZones());

        }
        sendCommands();


    }

    void SpreadDronesOnZones(List<Drone> drones, List<Zone> zones) {
        HashMap<Drone, Zone> targets_drone = Utils.FindBestZone(drones, zones);
        for (int k = 0 ; k < drones.size();k++) {
            for(Zone zone : zones) {
                Zone target_zone;
                Drone target_drone;
                if(!free_zone.get(zone)) continue;
                for(int i = 0; i <  zone.max_enemy_drones+1;i++) {
                    boolean findDrone = false;
                    for (Map.Entry<Drone,Zone> key_value : targets_drone.entrySet()) {
                        target_zone = key_value.getValue();
                        target_drone = key_value.getKey();
                        if(!free_dron.get(target_drone)) continue;
                        if(zone.equals(target_zone)) {
                            setCommands(target_drone,zone);
                            findDrone = true;
                        }
                    }
                    if(!findDrone) {
                        for (Drone drone : drones) {
                            if(!free_dron.get(drone)) continue;
                            setCommands(drone,zone);
                            break;
                        }
                    }
                }
            }
        }
    }


    ArrayList<Zone> getMyZones() {
        ArrayList<Zone> result = new ArrayList<Zone>();
        for(Zone zone : zones) {
            if(zone.owner != myTeamIdx) continue;
            result.add(zone);
        }
        return result;
    }

    ArrayList<Zone> getNeutralZones() {
        ArrayList<Zone> result = new ArrayList<Zone>();
        for(Zone zone : zones) {
            if(zone.owner != -1) continue;
            result.add(zone);
        }
        return result;
    }

    ArrayList<Zone> getEnemyZones() {
        ArrayList<Zone> result = new ArrayList<Zone>();
        for(Zone zone : zones) {
            if(zone.owner == myTeamIdx) continue;
            result.add(zone);
        }
        Collections.sort(result, new Comparator<Zone>() {
            @Override
            public int compare(Zone o1, Zone o2) {
                int result = 0;
                if(o1.max_enemy_drones == o2.max_enemy_drones) result = 0;
                if(o1.max_enemy_drones < o2.max_enemy_drones) result = -1;
                if(o1.max_enemy_drones > o2.max_enemy_drones) result = 1;
                return result;
            }
        });
        return result;
    }

    ArrayList<Drone> getMyDronesInZone(Zone zone){
        ArrayList<Drone> result = new ArrayList<Drone>();
        Team team = teams[myTeamIdx];
        for (Drone drone : team.drones) {
            if (DroneInZone(drone,zone)) result.add(drone);
        }
        return result;
    }

    int getMaxEnemyDronesInZone(Zone zone) {
        int result = 0;
        for (Team team : teams) {
            int numTeamDrones = 0;
            if (team.id == myTeamIdx) continue;
            for (Drone drone : team.drones) {
                if (DroneInZone(drone,zone)) numTeamDrones ++;
            }
            if (numTeamDrones > result) result = numTeamDrones;
        }
        return  result;
    }

    void setCommands(Drone drone,Zone zone) {
        free_zone.put(zone,false);
        free_dron.put(drone,false);
        commands[drone.id] = ""+zone.pos.x + " " + zone.pos.y;
    }

    void setCommands(Map<Drone,Zone> drone_targets, boolean sendNotfreeZone){
        Zone target_zone;
        Drone target_drone;
        for (Map.Entry<Drone,Zone> key_value : drone_targets.entrySet()) {
            target_zone = key_value.getValue();
            target_drone = key_value.getKey();
            if(target_zone == null) continue;
            //if(target_zone.owner == myTeamIdx) continue;
            if(!free_zone.get(target_zone) && !sendNotfreeZone) continue;
            /*
            free_zone.put(target_zone,false);
            free_dron.put(target_drone,false);
            commands[target_drone.id] = ""+target_zone.pos.x + " " + target_zone.pos.y;
            */
            setCommands(target_drone,target_zone);
        }
    }

    boolean DroneInZone(Drone drone,Zone zone) {
        return PointInZone(drone.pos,zone);
    }

    boolean PointInZone(Point point,Zone zone) {
        return point.x >= (zone.pos.x-100) && point.x <= (zone.pos.x+100) && point.y >= (zone.pos.y-100) && point.y <= (zone.pos.y+100);
    }

    void sendCommands() {
        ArrayList<Drone> free_drone  = getFreeDrone();
        Point center = centerZones();
        for (Drone drone: free_drone) {
            commands[drone.id] = "" + center.x + " " + center.y;
        }
        for (String command : commands) {
            System.out.println(command);
        }
    }

    Point centerZones() {
        List<Point> points = new ArrayList<Point>();
        for (Zone zone : zones) {
            points.add(zone.pos);
        }
        return Utils.getCenter(points);
    }

    ArrayList<Drone> getFreeDrone() {
        ArrayList<Drone> list_drone= new ArrayList<Drone>();
        for (Map.Entry<Drone,Boolean> key_value: free_dron.entrySet()) {
            if(!key_value.getValue()) continue;
            list_drone.add(key_value.getKey());
        }
        return list_drone;
    }

    ArrayList<Zone> getFreeZone() {
        ArrayList<Zone> list_zone = new ArrayList<Zone>();
        for(Map.Entry<Zone,Boolean> key_value : free_zone.entrySet()) {
            if(!key_value.getValue()) continue;
            list_zone.add(key_value.getKey());
        }
        return list_zone;
    }

    // program entry point
    public static void main(String args[]) throws InterruptedException {
        Scanner in = new Scanner(System.in);

        Player p = new Player();
        p.parseInit(in);

        while (true) {
            p.parseContext(in);
            p.play();
        }
    }

    // parse games data (one time at the beginning of the game: P I D Z...)
    void parseInit(Scanner in) {
        phase = Phase.Capture;
        teams = new Team[in.nextInt()]; // P
        myTeamIdx = in.nextInt(); // I

        int cntDronesPerTeam = in.nextInt(); // D
        for (int i = 0; i < teams.length; i++) {
            Team t = new Team();
            t.id = i;
            t.drones = new Drone[cntDronesPerTeam];
            for (int j =0 ;j < cntDronesPerTeam;j++) {
                t.drones[j] = new Drone(j,i);
            }
            teams[i] = t;
        }

        commands = new String[cntDronesPerTeam];

       /* for (Drone my_dron : teams[myTeamIdx].drones ){
            free_dron.put(my_dron,true);
        }*/

        zones = new Zone[in.nextInt()]; // Z

        for (int i = 0; i < zones.length; i++) {
            Zone z = new Zone();
            z.pos.x = in.nextInt();
            z.pos.y = in.nextInt();
            z.id = i;
            zones[i] = z;
        }

       /* for (Zone zone : zones) {
            free_zone.put(zone,true);
        }*/
    }

    // parse contextual data to update the game's state (called for each game round)
    void parseContext(Scanner in) {
        for (Zone z : zones) {
            z.owner = in.nextInt(); // update zones owner
            z.count_drone_owner = 0;
        }

        for (Team t : teams ) {
            for (int j = 0; j < t.drones.length; j++) {
                t.drones[j].pos.move(in.nextInt(), in.nextInt()); // update drones position
            }
        }
        for (Zone z : zones) {
            z.max_enemy_drones = getMaxEnemyDronesInZone(z);
            z.myDronesInZone = getMyDronesInZone(z);
            if(z.owner == -1) continue;
            for(Drone drone : teams[z.owner].drones) {
                if (DroneInZone(drone,z)) z.count_drone_owner ++;
            }
        }


        for (Drone my_dron : teams[myTeamIdx].drones ){
            free_dron.put(my_dron,true);
        }

        for (Zone zone : zones) {
            free_zone.put(zone,true);
        }
    }
}
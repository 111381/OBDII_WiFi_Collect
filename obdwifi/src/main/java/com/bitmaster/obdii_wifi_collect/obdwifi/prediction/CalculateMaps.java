package com.bitmaster.obdii_wifi_collect.obdwifi.prediction;

/**
 * Created by renet on 5/13/14.
 */
public class CalculateMaps {

    public static double calculateRangeAtSpeed(int row) { //row==speed[km/h]

        int additionalRows = calculateDutyCycle(row);
        long sumOfRow = 0;
        long countOfSum = 0;
        for(int i = row-additionalRows; i <= row+additionalRows; i++) {
            for(int j = 0; j < MapCanValues.driveConsumption[i].length; j++) {
                sumOfRow += MapCanValues.driveConsumption[i][j] * MapCanValues.driveFrequency[i][j];
                countOfSum += MapCanValues.driveFrequency[i][j];
            }
        }
        double power = (double)sumOfRow/(double)countOfSum;
        double timeWithCapacity = (MapCanValues.CAPACITY * (MapCanValues.soc / 100.0)) / (-1 * power); //hours
        double range = (double)row * timeWithCapacity;
        //String output = Integer.toString(row) + "," + Double.toString(timeWithCapacity) + "," + Double.toString(range) + ";\n";
        return range;
    }

    public static double calculatePowerAtSpeed(int row) {

        int additionalRows = calculateDutyCycle(row);
        long sumOfRow = 0;
        long countOfSum = 0;
        for(int i = row-additionalRows; i <= row+additionalRows; i++) {
            for(int j = 0; j < MapCanValues.driveConsumption[i].length; j++) {
                sumOfRow += MapCanValues.driveConsumption[i][j] * MapCanValues.driveFrequency[i][j];
                countOfSum += MapCanValues.driveFrequency[i][j];
            }
        }
        return (double)sumOfRow/(double)countOfSum;
    }

    /**
     * If sum of writings to log row is smaller than row size, then calculation must cover more than one row (speed).
     * Then take one more row from both side and calculate over them.
     */
    private static int calculateDutyCycle(int row) {

        double dutyCycle;
        int additionalRows = -1;
        do {
            additionalRows++;
            if((row + additionalRows) >= MapCanValues.driveFrequency.length || (row - additionalRows) < 0) {
                return additionalRows - 1;
            }
            dutyCycle = 0.0;
            int determinedItems = 0;
            for(int i = row-additionalRows; i <= row+additionalRows; i++) {
                for(int j = 0; j < MapCanValues.driveFrequency[i].length; j++) {
                    determinedItems += MapCanValues.driveFrequency[i][j];
                }
            }
            dutyCycle = (double)determinedItems / (double)(MapCanValues.driveFrequency[row].length);
        } while(dutyCycle < 1);

        return additionalRows;
    }

}

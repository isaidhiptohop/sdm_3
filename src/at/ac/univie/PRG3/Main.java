package at.ac.univie.PRG3;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

public class Main {

    String path = "LSH data.txt";
    double[] data;
    double[] centroids, tmpCentroids;
    int[] clusterIDs, numClusterPoints;
    int points;
    final int clusters = 15;
    final int dimension = 10;

    public void loadData(String path) {
        try {
            String raw = new String(Files.readAllBytes(Paths.get(path)));
            String [] sp = raw.split("(,|\n)");
            data = new double[sp.length];
            centroids = new double[clusters * dimension];
            tmpCentroids = new double[clusters * dimension];
            points = sp.length /dimension;
            clusterIDs = new int[points];
            numClusterPoints = new int[clusters];


            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;

            for (int i = 0; i< sp.length; ++i) {
                data[i] = Double.parseDouble(sp[i]);
                if(data[i] < min)
                    min = data[i];
                if(data[i] > max)
                    max = data[i];
            }


            Random rng = new Random();
            //starting centroids
            for(int i = 0; i < clusters * dimension; ++i) {
                centroids[i] = rng.nextDouble() * (max - min) + min;
            }

        } catch (Exception e) {
            System.err.println("can't read file.");
            System.exit(1);
        }
    }

    private void saveData(String path) {
        File file = new File(path);
        FileOutputStream fos = null;
        try {
            if (!file.exists())
                file.createNewFile();

            fos = new FileOutputStream(file);
            for(int i = 0; i < points; ++i) {
                String s = "";
                for(int j = 0; j < dimension; ++j) {
                    s += data[i * dimension + j] + ",";
                }
                s += clusterIDs[i] + "\n";
                fos.write(s.getBytes());
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(fos != null) {
                try {
                    fos.flush();
                    fos.close();
                } catch (Exception ignored) {}
            }
        }
    }

    private double distance(int dataPoint, int centroid) {
        double tmp, dist = 0;
        for(int i = 0; i < dimension; ++i) {
            tmp = data[dataPoint + i] - centroids[centroid + i];
            dist += tmp*tmp;
        }
        return dist;
    }

    private void kmeans() {
        double minDist, tmp;
        int clusterID;
        boolean converged = false;

        for(int i = 0; i < tmpCentroids.length; ++i) {
            tmpCentroids[i] = 0;
        }

        while (!converged) {
            converged = true;

            for(int i = 0; i < numClusterPoints.length; ++i)
                numClusterPoints[i] = 0;

            for (int i = 0; i < points; ++i) {
                minDist = distance(i * dimension, 0);
                clusterID = 0;
                for (int j = 1; j < clusters; ++j) {
                    tmp = distance(i * dimension, j * dimension);
                    if (tmp < minDist) {
                        minDist = tmp;
                        clusterID = j;
                    }
                }
                if(clusterIDs[i] != clusterID) {
                    converged = false;
                    clusterIDs[i] = clusterID;
                }

                for(int k = 0; k < dimension; ++k) {
                    tmpCentroids[clusterID * dimension + k] += data[i * dimension + k];
                }
                ++numClusterPoints[clusterID];
            }


            for(int i = 0; i < tmpCentroids.length; ++i) {
                if(numClusterPoints[i/dimension] != 0) {
                    centroids[i] = tmpCentroids[i] / numClusterPoints[i / dimension];
                    tmpCentroids[i] = 0;
                }
            }
        }
    }

    public static void main(String[] args) {
        Main prg = new Main();
        prg.run(args);
    }

    private void printClusters() {
        for (int i = 0; i< clusters; ++i) {
            System.out.println("cluster " + i + ": ");
            for(int j = 0; j < dimension; ++j) {
                System.out.println("   " + centroids[i* dimension + j]);
            }

        }

    }

    public void run(String[] args) {
        loadData(path);
        long start, end;

        System.out.println("calculation started (n = " + points + ")");
        start = System.nanoTime();
        kmeans();
        end = System.nanoTime();

        long duration = end - start;
        System.out.println("calculation finished, duration = " + (duration / 1.0e9) +  "s");
        saveData(path + ".out");

    }
}

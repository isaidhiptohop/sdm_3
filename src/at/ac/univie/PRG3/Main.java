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
    Bucket[][] centroidHashes;
    int dataHash[];
    int[] clusterHashes;

    double bucketSize = 10;
//    final double threshold = 100;

    final int funcGroups = 4;
    final int funcs = funcGroups * funcGroups;
    double[] fHash;

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
            fHash = new double[funcs * dimension];
            centroidHashes = new Bucket[funcs][clusters];
            dataHash = new int[funcs*points];
            clusterHashes = new int[funcs * clusters];

            for(int i = 0; i < funcs; ++i) {
                for(int j = 0; j < clusters; ++j) {
                    centroidHashes[i][j] = new Bucket(-1,-1);
                }
            }


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

            for(int i = 0; i < funcs * dimension; ++i) {
                fHash[i] = (rng.nextDouble()-0.5) *2;
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

    private int hash(int point, boolean isData, int func) {
        double hashed = 0;
        if(isData) {
            for(int i = 0; i < dimension; ++i) {
                hashed += data[point + i] * fHash[func * dimension + i];
            }
        } else {
            for(int i = 0; i < dimension; ++i) {
                hashed += centroids[point + i] * fHash[func * dimension + i];
            }
        }
        hashed = hashed / bucketSize;

        return (int)Math.floor(hashed);
    }

    private void kmeansLsh() {
        double minDist = Double.MAX_VALUE, tmp;
        int clusterID = -1, changed;
        boolean converged = false;

        for(int i = 0; i < tmpCentroids.length; ++i) {
            tmpCentroids[i] = 0;
        }

        for(int i = 0; i < points; ++i) {
            for(int j = 0; j < funcs; ++j) {
                dataHash[i * funcs + j] = hash(i * dimension, true, j);
            }
        }

        while (!converged) {
            converged = true;
            changed = 0;

            for(int i = 0; i < numClusterPoints.length; ++i)
                numClusterPoints[i] = 0;

            // new ...
            for(int i = 0; i < clusters; ++i) {
                for (int j = 0; j < funcs; ++j) {
                    clusterHashes[i*funcs + j] = hash(i * dimension, false, j);
                }
            }

            // ... till here

            for (int i = 0; i < points; ++i) {
                // new ...
                clusterID = -1;
                for (int j = 0; j < funcs; ++j) {
                    dataHash[j] = hash(i * dimension, true, j);
                    for(int k = 0; k < clusters; ++k) {
                        if(dataHash[j] < centroidHashes[j][k].max) {
                            //System.out.println(dataHashV[j] + " : " + centroidHashes[j][k].max);
                            if(clusterID == -1) {
                                clusterID = centroidHashes[j][k].cluster;
                            } else if(clusterID != centroidHashes[j][k].cluster) { //if in func group different cluster
                                j = ((j/funcGroups) + 1) * funcGroups; //set to begin of next func group
                                clusterID = -1;
                                if(j >= funcs)
                                    break;
                                continue;
                            } else if((j + 1) % funcGroups == 0) { //if full func group is passed
                                break; //abort calc
                            }
                        }
                    }
                }



/*                minDist = Math.abs(centroidHashes[0] - dataHashV);
                for (int j = 1; j < clusters; ++j) {
                    tmp = Math.abs(centroidHashes[j] - dataHashV);
                    if(tmp < minDist) {
                        minDist = tmp;
                        clusterID = j;
                    }
                }

                minDist = distance(i * dimension, clusterID * dimension);
                // ... till here
*/


//                if (minDist < distance(i * dimension, clusterIDs[i] * dimension)) { // new

                    if (clusterIDs[i] != clusterID) {
                        converged = false;
                        clusterIDs[i] = clusterID;
                        ++changed;
                    }

//                } // new

                if(clusterID != -1) {
                    for (int k = 0; k < dimension; ++k) {
                        tmpCentroids[clusterID * dimension + k] += data[i * dimension + k];
                    }
                    ++numClusterPoints[clusterID];
                }
            }


            for(int i = 0; i < tmpCentroids.length; ++i) {
                if(numClusterPoints[i/dimension] != 0) {
                    centroids[i] = tmpCentroids[i] / numClusterPoints[i / dimension];
                    tmpCentroids[i] = 0;
                }
            }
            System.out.println("changed: " + changed);
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
        kmeansLsh();
        end = System.nanoTime();

        long duration = end - start;
        System.out.println("calculation finished, duration = " + (duration / 1.0e9) +  "s");
        saveData(path + ".out");

    }
}

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * A utility class for finding wither rooms - bedrock formations in the ceiling of the nether suitable for killing the
 * wither safely.
 */
@SuppressWarnings("WeakerAccess")
public class WitherRoomFinder {

    public static void main(String... args) {
        Pos target;
        int chunkR;
        try {
            target = new Pos(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
            chunkR = Integer.parseInt(args[2]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            printUsage();
            System.exit(1);
            return;
        }
        Pos centerChunk = target.toChunkPos();
        System.out.printf("Finding wither room near block %s in chunk %s within chunk radius (%d)\n", target, centerChunk, chunkR);
        System.out.println();
        int chunkD = chunkR * 2;
        int blockD = chunkD * 16;
        int[][] bedrockHeights = new int[blockD][blockD];
        System.out.printf("Generating %1$d x %1$d area of bedrock\n", blockD);
        for (int chunkOffsetX = 0; chunkOffsetX < chunkD; chunkOffsetX++) {
            for (int chunkOffsetZ = 0; chunkOffsetZ < chunkD; chunkOffsetZ++) {
                Pos chunkOffset = new Pos(chunkOffsetX, chunkOffsetZ);
                Pos chunkPos = centerChunk.plus(chunkOffset).translate(-chunkR);
                System.out.println("\tCalculating bedrock for chunk " + chunkPos);
                calculateLowestBedrockInChunk(bedrockHeights, chunkOffset.toBlockPos(), chunkPos);
            }
        }
        System.out.println("Generation complete!");
        System.out.println();
        System.out.println("Finding Wither rooms");
        List<Pos> witherRooms = findWitherRoomsWithinRadius(bedrockHeights);
        Pos blockOffset = centerChunk.translate(-chunkR).toBlockPos();
        Optional<Pos> closestWitherRoom = witherRooms.stream()
                .map(localPos -> localPos.plus(blockOffset))
                .peek(room -> System.out.println("\t Found wither room centered on " + room))
                .reduce((a, b) -> b.squareDist(target) < a.squareDist(target) ? b : a);


        if (closestWitherRoom.isPresent()) {
            System.out.println("Closest room is centered on " + closestWitherRoom.get());
        } else {
            System.out.println("Failed to find room, try increasing the radius.");
        }
    }


    /**
     * Generate the bedrock for a chunk and store the lowest y-coordinate for each column in a 2d array at {@code blockOffset}.
     *
     * @param bedrockHeights the array to store the results in
     * @param blockOffset    the offset to store the results for the chunk
     * @param chunkPos       the position of the chunk (in chunk coordinates)
     */
    private static void calculateLowestBedrockInChunk(int[][] bedrockHeights, Pos blockOffset, Pos chunkPos) {
        // Seed selection based off what Minecraft would do using magic numbers
        Random rand = new Random(((long) chunkPos.x) * 341873128712L + ((long) chunkPos.z) * 132897987541L);
        for (int z = 0; z < 16; ++z) {
            for (int x = 0; x < 16; ++x) {
                // Superfluous usages of the nextDouble to keep this algorithm in sync with what
                // Minecraft would do with its its RNG
                rand.nextDouble();
                rand.nextDouble();
                rand.nextDouble();

                // We have to interate through every y to keep the RNG in sync with Minecraft
                for (int y = 127; y >= 0; --y) {
                    if (y >= 127 - rand.nextInt(5)) {
                        bedrockHeights[blockOffset.z + z][blockOffset.x + x] = y;
                    } else {
                        // Superfluous usages of the nextInt to keep this algorithm in sync with Minecraft
                        rand.nextInt(5);
                    }
                }
            }
        }
    }

    /**
     * Calculates the Wither rooms within a populated 2d array of bedrock heights.
     * <p>
     * A wither room is defined as a 3x3 bedrock formation all with the same y-coordinate.
     *
     * @param bedrockHeights a 2d array representing the lowest bedrock y-coordinate in section of the nether ceiling
     * @return a list of positions, relative to the array, which correspond to wither room locations
     */
    private static List<Pos> findWitherRoomsWithinRadius(int[][] bedrockHeights) {
        int blockD = bedrockHeights.length;
        List<Pos> witherKillingRooms = new ArrayList<>();
        for (int row3 = blockD - 1; row3 > 3; row3--) {
            for (int col3 = blockD - 1; col3 > 3; col3--) {
                int col2 = col3 - 1;
                int col1 = col2 - 1;
                if (bedrockHeights[row3][col1] == bedrockHeights[row3][col2]) {
                    if (bedrockHeights[row3][col2] == bedrockHeights[row3][col3]) {
                        // check the other two rows
                        int row2 = row3 - 1;
                        int row1 = row2 - 1;
                        if (bedrockHeights[row3][col3] == bedrockHeights[row2][col3] &&
                                bedrockHeights[row3][col3] == bedrockHeights[row2][col2] &&
                                bedrockHeights[row3][col3] == bedrockHeights[row2][col1]) {
                            if (bedrockHeights[row3][col3] == bedrockHeights[row1][col3] &&
                                    bedrockHeights[row3][col3] == bedrockHeights[row1][col2] &&
                                    bedrockHeights[row3][col3] == bedrockHeights[row1][col1]) {

                                Pos center = new Pos(col2, row2);
                                witherKillingRooms.add(center);
                            }
                        }
                    } else {
                        // this isn't a match, but we can't make an optimisation to skip one
                        continue;
                    }
                } else {
                    // We can skip ahead by 2 as we know the next one won't be a match either
                    col3--;
                }
            }
        }
        return witherKillingRooms;
    }

    private static void printUsage() {
        System.out.println("usage: java WitherRoomFinder blockX blockZ chunkRadius");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("\tblockX\t\t- The x axis coordinate for the block to start the scan from");
        System.out.println("\tblockZ\t\t- The z axis coordinate for the block to start the scan from");
        System.out.println("\tchunkRadius\t- The radius of chunks around the block coordinate to scan for a room");
    }

    /**
     * A representation of a x/z position within a Minecraft world
     */
    public static class Pos {

        public final int x;
        public final int z;

        public Pos(int x, int z) {
            this.x = x;
            this.z = z;
        }

        /**
         * @return a new {@link Pos} translated to block coordinates (from chunk coordinates)
         */
        public Pos toBlockPos() {
            return new Pos(x << 4, z << 4);
        }

        /**
         * @return a new {@link Pos} translated to chunk coordinates (from block coordinates)
         */
        public Pos toChunkPos() {
            return new Pos(x >> 4, z >> 4);
        }

        /**
         * @return a new {@link Pos} translated by a delta on both the x and z axis
         */
        public Pos translate(int d) {
            return translate(d, d);
        }

        /**
         * @return a new {@link Pos} translated by {@code dX} on the x axis and {@code dZ} on the z axis
         */
        public Pos translate(int dX, int dZ) {
            return new Pos(x + dX, z + dZ);
        }

        public Pos plus(Pos other) {
            return translate(other.x, other.z);
        }

        public int squareDist(Pos other) {
            int dX = x - other.x;
            int dZ = z - other.z;
            return dX * dX + dZ * dZ;
        }

        @Override
        public String toString() {
            return "(" + x + ", " + z + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Pos pos = (Pos) o;
            return x == pos.x &&
                    z == pos.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, z);
        }
    }

}

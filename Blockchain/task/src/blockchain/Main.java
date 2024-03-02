package blockchain;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {

    private static transient byte[] publicKey;
    private static transient byte[] privateKey;

    private static transient int prefix = 0;
    private static int MsgID = 0;

    private static volatile List<Block> mainList;
    private static final transient List<Message> chat = new ArrayList<>();
    private static final transient List<String> users = List.of("Shop1", "Shop2", "Shop3", "Shop4");

    private static final transient Long REWARD = 100L;


    public static void main(String[] args) throws Exception {

        if (Files.exists(Path.of("block.dat"))) {
            mainList = (List<Block>) SerializationUtils.deserialize("block.dat");
            printFromFile(mainList);
            return;
        }


        MessageCrypt keys = new MessageCrypt(1024);
        keys.createKeys();
        publicKey = keys.getPublicKey().getEncoded();
        privateKey = keys.getPrivateKey().getEncoded();

        mainList = new ArrayList<>();

        initBlockchain();
    }

    public static List<Block> initBlockchain() throws ExecutionException, InterruptedException, IOException {

        int j = mainList.size();
        MsgID += mainList.stream().flatMapToLong(b -> b.getData().stream().mapToLong(Message::getMessageId))
                .max().orElse(0L);

        int processors = Runtime.getRuntime().availableProcessors();

        ExecutorService executorMiner = Executors.newFixedThreadPool(processors);
        ExecutorService executorMsg = Executors.newFixedThreadPool(processors);


        for (int i = 1 + j; i <= 5; i++) {

            long timeStamp = new Date().getTime();
            String prevHash;

            if (i == 1) {
                prevHash = "0";
            } else {
                prevHash = mainList.get(i - 2).getHash();
            }

            Collection blockCallList = new ArrayList<>();
            Collection msgCallList = new ArrayList<>();

            for (int h = 1; h <= processors; h++) {
                blockCallList.add(new BlockInitializer(i, timeStamp, prevHash, chat, prefix, privateKey, REWARD));
            }

            users.forEach(u -> msgCallList.add(new Message(++MsgID, 35L, "", u, privateKey)));

            Object tempBlockListFirst = executorMiner.invokeAny(blockCallList);
            List<Future<Message>> tempMsgList = executorMsg.invokeAll(msgCallList);

            Block minedBlock = (Block) tempBlockListFirst;

            if (Utils.validateBlock(minedBlock)) {

                // Skip code below to increase mining speed (for tests purpose only)
                if ((minedBlock.getTotalTime()) / 1000.0 < 15) {
                    prefix++;
                } else if ((minedBlock.getTotalTime()) / 1000.0 > 15) {
                    prefix--;
                }

                minedBlock.setN(minedBlock, prefix);
                mainList.add(minedBlock);
            }

            chat.clear();
            for (Future<Message> m : tempMsgList) {
                chat.add(m.get());
            }

            print(mainList.get(i - 1));
        }

        SerializationUtils.serialize(mainList, "block.dat");

        executorMiner.shutdownNow();
        executorMsg.shutdownNow();

        return mainList;
    }

    public static void print(Block block) {
        System.out.println(block.toString());
    }

    public static void printFromFile(List<Block> mainList) {
        for (Block b : mainList) {
            System.out.println(b.toString());
        }
    }

}

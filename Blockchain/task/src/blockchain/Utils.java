package blockchain;

import java.util.List;

public class Utils {

    public static boolean validateBlockChain(List<Block> input) {
        return input.stream().allMatch(b -> StringUtil.applySha256(b.getId() + b.getTimestamp() + b.getPrevHash() +
                b.getData() + b.getSeed()).equals(b.getHash()));
    }

    public static boolean validateBlock(Block vS) {
        String compare = StringUtil.applySha256(vS.getId() + vS.getTimestamp() + vS.getPrevHash() +
                vS.getData() + vS.getSeed());

        return compare.equals(vS.getHash());
    }

    public static Long allTransactionsCount(List<Block> blockChain, String fromName) {
        return blockChain.stream().flatMapToLong(b ->
                b.getData().stream().filter(m -> fromName.equals(m.getFromName())).mapToLong(Message::getSentAmount))
                .sum();
    }

    public static Long balanceCount(List<Block> blockChain, String name) {
        return blockChain.stream().flatMapToLong(b ->
                b.getData().stream().filter(m -> name.equals(m.getToName())).mapToLong(Message::getSentAmount))
                .sum() - allTransactionsCount(blockChain, name);
    }
}

package org.tron;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.tron.utils.AddressUtil;
import org.tron.utils.TransactionUtil;

import java.util.Map;

/**
 * @author <a href="mailto:HelloHeSir@gmail.com">HeC</a>
 * 2022/6/28 2:47 PM
 */
public class Test {

    public static void main(String[] args) {
        // 离线生成地址
        Map<String, String> address = AddressUtil.createAddress();
        System.out.println(JSON.toJSONString(address));
        // hex地址互转
        System.out.println(AddressUtil.toViewAddress("41ea51342dabbb928ae1e576bd39eff8aaf070a8c6"));
        System.out.println(AddressUtil.toHexAddress("TXLAQ63Xg1NAzckPwKHvzw7CSEmLMEqcdj"));
        // 归集 parameter
        String addressParam = addZero("41eab476ff352d00eb38fb8a85c78c854365faf80b", 64);
        String amountParam = addZero(Long.toString(100000, 16), 64);
        System.out.println(addressParam + amountParam);
        // 余额查询 parameter
        String addressParam1 = addZero("41eab476ff352d00eb38fb8a85c78c854365faf80b".substring(2), 64);
        System.out.println(addressParam1);
        // 离线签名
        JSONObject transaction = TransactionUtil.getTransaction("{\n" +
                "    \"result\": {\n" +
                "        \"result\": true\n" +
                "    },\n" +
                "    \"transaction\": {\n" +
                "        \"visible\": false,\n" +
                "        \"txID\": \"90588dec9d31e68d6f47d941d6f160c73859c9c2725f0d6bf2c7724df0db484a\",\n" +
                "        \"raw_data\": {\n" +
                "            \"contract\": [\n" +
                "                {\n" +
                "                    \"parameter\": {\n" +
                "                        \"value\": {\n" +
                "                            \"data\": \"a9059cbb000000000000000000000041eab476ff352d00eb38fb8a85c78c854365faf80b00000000000000000000000000000000000000000000000000000000000186a0\",\n" +
                "                            \"owner_address\": \"41d1e7a6bc354106cb410e65ff8b181c600ff14292\",\n" +
                "                            \"contract_address\": \"41ea51342dabbb928ae1e576bd39eff8aaf070a8c6\"\n" +
                "                        },\n" +
                "                        \"type_url\": \"type.googleapis.com/protocol.TriggerSmartContract\"\n" +
                "                    },\n" +
                "                    \"type\": \"TriggerSmartContract\"\n" +
                "                }\n" +
                "            ],\n" +
                "            \"ref_block_bytes\": \"a22a\",\n" +
                "            \"ref_block_hash\": \"d122ad5df4c4d482\",\n" +
                "            \"expiration\": 1656488109000,\n" +
                "            \"fee_limit\": 10000000,\n" +
                "            \"timestamp\": 1656488049665\n" +
                "        },\n" +
                "        \"raw_data_hex\": \"0a02a22a2208d122ad5df4c4d48240c8c78af39a305aae01081f12a9010a31747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e54726967676572536d617274436f6e747261637412740a1541d1e7a6bc354106cb410e65ff8b181c600ff14292121541ea51342dabbb928ae1e576bd39eff8aaf070a8c62244a9059cbb000000000000000000000041eab476ff352d00eb38fb8a85c78c854365faf80b00000000000000000000000000000000000000000000000000000000000186a07081f886f39a30900180ade204\"\n" +
                "    }\n" +
                "}");
        System.out.println(transaction.toJSONString());
    }

    private static String addZero(String dt, int length) {
        StringBuilder builder = new StringBuilder();
        int zeroAmount = length - dt.length();
        for (int i = 0; i < zeroAmount; i++) {
            builder.append("0");
        }
        builder.append(dt);
        return builder.toString();
    }

}

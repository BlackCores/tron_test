/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.crypto.Hash;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignUtils;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionSign;
import org.tron.protos.contract.SmartContractOuterClass;

import java.lang.reflect.Constructor;

@Slf4j
public class TransactionUtil {

    public static void main(String[] args) {
        System.out.println(getTransaction("{\"privateKey\":\"20a98cb4c3dc56132e4d262bdf3f2ff6d590d57d14b3f5632d50c7e03a7a611e\",\"transaction\":{\"raw_data\":{\"contract\":[{\"parameter\":{\"value\":{\"data\":\"a9059cbb00000000000000000000004189851eaebd526a08da0435df60a1bcd86fdad99a00000000000000000000000000000000000000000000000000000000000186a0\",\"owner_address\":\"41d0673873c33aba869aeff579ed79c1b6acacaae8\",\"contract_address\":\"41ea51342dabbb928ae1e576bd39eff8aaf070a8c6\"},\"type_url\":\"type.googleapis.com/protocol.TriggerSmartContract\"},\"type\":\"TriggerSmartContract\"}],\"ref_block_bytes\":\"05ca\",\"ref_block_hash\":\"df2600653370497a\",\"expiration\":1655969475000,\"fee_limit\":5000000,\"timestamp\":1655969415217,\"data\":\"73656e6420636f6c6c656374\"},\"raw_data_hex\":\"0a0205ca2208df2600653370497a40b8d3e3fb98305aae01081f12a9010a31747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e54726967676572536d617274436f6e747261637412740a1541d0673873c33aba869aeff579ed79c1b6acacaae8121541ea51342dabbb928ae1e576bd39eff8aaf070a8c62244a9059cbb00000000000000000000004189851eaebd526a08da0435df60a1bcd86fdad99a00000000000000000000000000000000000000000000000000000000000186a070b180e0fb98309001c096b102\",\"txID\":\"4dc0538a0350da89b797325985dacddc40f0dd24fe04b9d7f6bcfa9cbed20bea\",\"visible\":false}}"));
    }

    public static JSONObject getTransaction(String contract) {
        JSONObject input = JSONObject.parseObject(contract);
        String strTransaction = input.getJSONObject("transaction").toJSONString();
        Protocol.Transaction transaction = packTransaction(strTransaction);
        JSONObject jsonTransaction = JSONObject.parseObject(JsonFormat.printToString(transaction));
        input.put("transaction", jsonTransaction);
        Protocol.TransactionSign.Builder build = Protocol.TransactionSign.newBuilder();
        try {
            JsonFormat.merge(input.toJSONString(), build);
        } catch (JsonFormat.ParseException e) {
            log.error(e.getMessage(), e);
        }
        Protocol.Transaction reply = TransactionUtil.sign(build.build());
        return printCreateTransaction(reply);
    }

    public static Protocol.Transaction sign(TransactionSign transactionSign) {
        byte[] privateKey = transactionSign.getPrivateKey().toByteArray();
        Protocol.Transaction transaction = transactionSign.getTransaction();
        SignInterface cryptoEngine = SignUtils
                .fromPrivate(privateKey, true);
        ByteString sig = ByteString.copyFrom(cryptoEngine.Base64toBytes(cryptoEngine
                .signHash(getRawHash(transaction).getBytes())));
        return transaction.toBuilder().addSignature(sig).build();
    }

    private static Sha256Hash getRawHash(Protocol.Transaction transaction) {
        return Sha256Hash.of(true, transaction.getRawData().toByteArray());
    }

    public static JSONObject printCreateTransaction(Protocol.Transaction transaction) {
        JSONObject jsonObject = printTransactionToJSON(transaction);
        jsonObject.put("visible", false);
        return jsonObject;
    }

    public static byte[] generateContractAddress(Protocol.Transaction trx, byte[] ownerAddress) {
        // get tx hash
        byte[] txRawDataHash = Sha256Hash
                .of(true, trx.getRawData().toByteArray())
                .getBytes();

        // combine
        byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
        System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
        System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);

        return Hash.sha3omit12(combined);
    }


    public static JSONObject printTransactionToJSON(Protocol.Transaction transaction) {
        JSONObject jsonTransaction = JSONObject
                .parseObject(JsonFormat.printToString(transaction));
        JSONArray contracts = new JSONArray();
        transaction.getRawData().getContractList().stream().forEach(contract -> {
            try {
                JSONObject contractJson = null;
                Any contractParameter = contract.getParameter();
                switch (contract.getType()) {
                    case CreateSmartContract:
                        SmartContractOuterClass.CreateSmartContract deployContract = contractParameter
                                .unpack(SmartContractOuterClass.CreateSmartContract.class);
                        contractJson = JSONObject
                                .parseObject(JsonFormat.printToString(deployContract));
                        byte[] ownerAddress = deployContract.getOwnerAddress().toByteArray();
                        byte[] contractAddress = generateContractAddress(transaction, ownerAddress);
                        jsonTransaction.put("contract_address", ByteArray.toHexString(contractAddress));
                        break;
                    default:
                        Class clazz = SmartContractOuterClass.TriggerSmartContract.class;
                        contractJson = JSONObject
                                .parseObject(JsonFormat.printToString(contractParameter.unpack(clazz)));
                        break;
                }

                JSONObject parameter = new JSONObject();
                parameter.put("value", contractJson);
                parameter.put("type_url", contract.getParameterOrBuilder().getTypeUrl());
                JSONObject jsonContract = new JSONObject();
                jsonContract.put("parameter", parameter);
                jsonContract.put("type", contract.getType());
                if (contract.getPermissionId() > 0) {
                    jsonContract.put("Permission_id", contract.getPermissionId());
                }
                contracts.add(jsonContract);
            } catch (InvalidProtocolBufferException e) {
                log.debug("InvalidProtocolBufferException: {}", e.getMessage());
            }
        });

        JSONObject rawData = JSONObject.parseObject(jsonTransaction.get("raw_data").toString());
        rawData.put("contract", contracts);
        jsonTransaction.put("raw_data", rawData);
        String rawDataHex = ByteArray.toHexString(transaction.getRawData().toByteArray());
        jsonTransaction.put("raw_data_hex", rawDataHex);
        String txID = ByteArray.toHexString(Sha256Hash
                .hash(true,
                        transaction.getRawData().toByteArray()));
        jsonTransaction.put("txID", txID);
        return jsonTransaction;
    }

    public static Protocol.Transaction packTransaction(String strTransaction) {
        JSONObject jsonTransaction = JSON.parseObject(strTransaction);
        JSONObject rawData = jsonTransaction.getJSONObject("raw_data");
        JSONArray contracts = new JSONArray();
        JSONArray rawContractArray = rawData.getJSONArray("contract");

        String contractType = null;
        for (int i = 0; i < rawContractArray.size(); i++) {
            try {
                JSONObject contract = rawContractArray.getJSONObject(i);
                JSONObject parameter = contract.getJSONObject("parameter");
                contractType = contract.getString("type");
                if (StringUtils.isEmpty(contractType)) {
                    log.debug("no type in the transaction, ignore");
                    continue;
                }
                Constructor<SmartContractOuterClass.TriggerSmartContract> constructor = SmartContractOuterClass.TriggerSmartContract.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                GeneratedMessageV3 generatedMessageV3 = constructor.newInstance();
                Message.Builder builder = generatedMessageV3.toBuilder();
                JsonFormat.merge(parameter.getJSONObject("value").toJSONString(), builder);
                Any any = Any.pack(builder.build());
                String value = ByteArray.toHexString(any.getValue().toByteArray());
                parameter.put("value", value);
                contract.put("parameter", parameter);
                contracts.add(contract);
            } catch (IllegalArgumentException e) {
                log.debug("invalid contractType: {}", contractType);
            } catch (Exception e) {
                log.error("", e);
            }
        }
        rawData.put("contract", contracts);
        jsonTransaction.put("raw_data", rawData);
        Protocol.Transaction.Builder transactionBuilder = Protocol.Transaction.newBuilder();
        try {
            JsonFormat.merge(jsonTransaction.toJSONString(), transactionBuilder);
            return transactionBuilder.build();
        } catch (JsonFormat.ParseException e) {
            log.debug("ParseException: {}", e.getMessage());
            return null;
        }
    }

}

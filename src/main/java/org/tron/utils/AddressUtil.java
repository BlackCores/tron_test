package org.tron.utils;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.crypto.ECKey;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:HelloHeSir@gmail.com">HeC</a>
 * 2022/5/29 8:26 PM
 */
public class AddressUtil {

    private static final int ADDRESS_SIZE = 21;
    private static final byte addressPreFixByte = (byte) 0x41; // 41 + address (byte) 0xa0; //a0 + address
    private static final SecureRandom random = new SecureRandom();

    public static void main(String[] args) {
        System.out.println(JSON.toJSONString(createAddress()));
    }

    public static Map<String, String> createAddress() {
        ECKey eCkey = new ECKey(random);
        String privateKey = ByteArray.toHexString(eCkey.getPrivKeyBytes());
        byte[] addressBytes = eCkey.getAddress();
        String hexAddress = ByteArray.toHexString(addressBytes);
        Map<String, String> addressInfo = new HashMap<>();
        addressInfo.put("address", toViewAddress(hexAddress));
        addressInfo.put("hexAddress", hexAddress);
        addressInfo.put("privateKey", privateKey);
        return addressInfo;
    }

    public static String toViewAddress(String hexAddress) {
        return encode58Check(ByteArray.fromHexString(hexAddress));
    }

    public static String toHexAddress(String tAddress) {
        if (StrUtil.isBlank(tAddress)) {
            return null;
        }
        if (tAddress.startsWith("T")) {
            return ByteArray.toHexString(decodeFromBase58Check(tAddress));
        }
        return tAddress;
    }

    public static String fromHexAddress(String address) {
        if (StrUtil.isBlank(address)) {
            return null;
        }
        if (address.startsWith("T")) {
            return address;
        }
        return encode58Check(ByteArray.fromHexString(address));
    }

    public static String encode58Check(byte[] input) {
        byte[] hash0 = Sha256Sm3Hash.hash(input);
        byte[] hash1 = Sha256Sm3Hash.hash(hash0);
        byte[] inputCheck = new byte[input.length + 4];
        System.arraycopy(input, 0, inputCheck, 0, input.length);
        System.arraycopy(hash1, 0, inputCheck, input.length, 4);
        return Base58.encode(inputCheck);
    }

    private static byte[] decodeFromBase58Check(String addressBase58) {
        if (StringUtils.isEmpty(addressBase58)) {
            return null;
        }
        byte[] address = decode58Check(addressBase58);
        if (!addressValid(address)) {
            return null;
        }
        return address;
    }

    private static byte[] decode58Check(String input) {
        byte[] decodeCheck = Base58.decode(input);
        if (decodeCheck.length <= 4) {
            return null;
        }
        byte[] decodeData = new byte[decodeCheck.length - 4];
        System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
        byte[] hash0 = Sha256Hash.hash(true, decodeData);
        byte[] hash1 = Sha256Hash.hash(true, hash0);
        if (hash1[0] == decodeCheck[decodeData.length] && hash1[1] == decodeCheck[decodeData.length + 1]
                && hash1[2] == decodeCheck[decodeData.length + 2] && hash1[3] == decodeCheck[decodeData.length + 3]) {
            return decodeData;
        }
        return null;
    }

    private static boolean addressValid(byte[] address) {
        if (ArrayUtils.isEmpty(address)) {
            return false;
        }
        if (address.length != ADDRESS_SIZE) {
            return false;
        }
        byte preFixbyte = address[0];
        return preFixbyte == getAddressPreFixByte();
        // Other rule;
    }

    private static byte getAddressPreFixByte() {
        return addressPreFixByte;
    }


}
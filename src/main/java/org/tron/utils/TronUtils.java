package org.tron.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.digests.SM3Digest;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * tron-utils
 *
 * @Autor TrickyZh 2021/1/20
 * @Date 2021-01-20 18:46:18
 */
@Slf4j
public class TronUtils {
    private static Logger logger = LoggerFactory.getLogger(TronUtils.class);
    static int ADDRESS_SIZE = 21;
    private static byte addressPreFixByte = (byte) 0x41; // 41 + address (byte) 0xa0; //a0 + address
    private static SecureRandom random = new SecureRandom();

    /**
     * 转换成hex地址
     *
     * @param address
     * @return
     */
    public static String toHexAddress(String address) {
        if (StringUtils.isEmpty(address)) {
            throw new IllegalArgumentException("传入的地址不可为空");
        }
        if (!address.startsWith("T")) {
            throw new IllegalArgumentException("传入地址不合法:" + address);
        }
        return Hex.toHexString(decodeFromBase58Check(address));
    }

    /**
     * 离线创建地址
     *
     * @return
     */
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

    /**
     * 根据私钥获取地址
     *
     * @param privateKey
     * @return
     */
    public static String getAddressByPrivateKey(String privateKey) {
        byte[] privateBytes = Hex.decode(privateKey);
        ECKey ecKey = ECKey.fromPrivate(privateBytes);
        byte[] from = ecKey.getAddress();
        return toViewAddress(Hex.toHexString(from));
    }

    /**
     * 转换成T开头的地址
     *
     * @param hexAddress
     * @return
     */
    public static String toViewAddress(String hexAddress) {
        return encode58Check(ByteArray.fromHexString(hexAddress));
    }

    public static String encode58Check(byte[] input) {
        try {
            byte[] hash0 = hash(true, input);
            byte[] hash1 = hash(true, hash0);
            byte[] inputCheck = new byte[input.length + 4];
            System.arraycopy(input, 0, inputCheck, 0, input.length);
            System.arraycopy(hash1, 0, inputCheck, input.length, 4);
            return Base58.encode(inputCheck);
        } catch (Throwable t) {
            log.error(String.format("data error:%s", Hex.toHexString(input)), t);
        }
        return null;
    }

    private static byte[] decode58Check(String input) throws Exception {
        byte[] decodeCheck = Base58.decode(input);
        if (decodeCheck.length <= 4) {
            return null;
        }
        byte[] decodeData = new byte[decodeCheck.length - 4];
        System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
        byte[] hash0 = hash(true, decodeData);
        byte[] hash1 = hash(true, hash0);
        if (hash1[0] == decodeCheck[decodeData.length] && hash1[1] == decodeCheck[decodeData.length + 1]
                && hash1[2] == decodeCheck[decodeData.length + 2] && hash1[3] == decodeCheck[decodeData.length + 3]) {
            return decodeData;
        }
        return null;
    }

    /**
     * Calculates the SHA-256 hash of the given bytes.
     *
     * @param input the bytes to hash
     * @return the hash (in big-endian order)
     */
    public static byte[] hash(boolean isSha256, byte[] input) throws NoSuchAlgorithmException {
        return hash(isSha256, input, 0, input.length);
    }

    /**
     * Calculates the SHA-256 hash of the given byte range.
     *
     * @param input  the array containing the bytes to hash
     * @param offset the offset within the array of the bytes to hash
     * @param length the number of bytes to hash
     * @return the hash (in big-endian order)
     */
    public static byte[] hash(boolean isSha256, byte[] input, int offset, int length) throws NoSuchAlgorithmException {
        if (isSha256) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(input, offset, length);
            return digest.digest();
        } else {
            SM3Digest digest = new SM3Digest();
            digest.update(input, offset, length);
            byte[] eHash = new byte[digest.getDigestSize()];
            digest.doFinal(eHash, 0);
            return eHash;
        }
    }


    public static byte[] decodeFromBase58Check(String addressBase58) {
        try {
            byte[] address = decode58Check(addressBase58);
            if (!addressValid(address)) {
                return null;
            }
            return address;
        } catch (Throwable t) {
            log.error(String.format("decodeFromBase58Check-error:" + addressBase58), t);
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
        return preFixbyte == addressPreFixByte;
        // Other rule;
    }

}

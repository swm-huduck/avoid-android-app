package com.huduck.application.bleCentral;


import static com.huduck.application.bleCentral.Constants.CHARACTERISTIC_UUID;
import static com.huduck.application.bleCentral.Constants.SERVICE_STRING;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothUtils {

    /**
     * Find characteristics of BLE
     * @param _gatt BluetoothGatt
     * @return list of found gatt characteristics
     */
    public static List<BluetoothGattCharacteristic> findBLECharacteristics(BluetoothGatt _gatt) {
        List<BluetoothGattCharacteristic> matching_characteristics = new ArrayList<>();
        List<BluetoothGattService> service_list = _gatt.getServices();
        BluetoothGattService service = findGattService(service_list);
        if (service == null) {
            return matching_characteristics;
        }

        List<BluetoothGattCharacteristic> characteristicList = service.getCharacteristics();
        for (BluetoothGattCharacteristic characteristic : characteristicList) {
            if (isMatchingCharacteristic(characteristic)) {
                matching_characteristics.add(characteristic);
            }
        }

        return matching_characteristics;
    }

    /**
     * Find the given uuid characteristic
     * @param _gatt gatt instance
     * @param _uuid_string uuid to query as string
     * @return
     */
    @Nullable
    public static BluetoothGattCharacteristic findCharacteristic(BluetoothGatt _gatt, String _uuid_string) {
        List<BluetoothGattService> service_list = _gatt.getServices();
        BluetoothGattService service = BluetoothUtils.findGattService(service_list);
        if (service == null) {
            return null;
        }

        List<BluetoothGattCharacteristic> characteristicList = service.getCharacteristics();
        for (BluetoothGattCharacteristic characteristic : characteristicList) {
            if (matchCharacteristic(characteristic, _uuid_string)) {
                return characteristic;
            }
        }

        return null;
    }

    /**
     * Match the given characteristic and a uuid string
     * @param _characteristic one of found characteristic provided by the server
     * @param _uuid_string uuid as string to match
     * @return true if matched
     */
    private static boolean matchCharacteristic(BluetoothGattCharacteristic _characteristic, String _uuid_string) {
        if (_characteristic == null) {
            return false;
        }
        UUID uuid = _characteristic.getUuid();
        return matchUUIDs(uuid.toString(), _uuid_string);
    }

    /**
     * Find Gatt service that matches with the server's service
     * @param _service_list list of services
     * @return matched service if found
     */
    @Nullable
    private static BluetoothGattService findGattService(List<BluetoothGattService> _service_list) {
        for (BluetoothGattService service : _service_list) {
            String service_uuid_string = service.getUuid().toString();
            if (matchServiceUUIDString(service_uuid_string)) {
                return service;
            }
        }
        return null;
    }

    /**
     * Try to match the given uuid with the service uuid
     * @param _service_uuid_string service UUID as string
     * @return true if service uuid is matched
     */
    private static boolean matchServiceUUIDString(String _service_uuid_string) {
        return matchUUIDs(_service_uuid_string, SERVICE_STRING);
    }

    /**
     * Check if there is any matching characteristic
     * @param _characteristic query characteristic
     */
    private static boolean isMatchingCharacteristic(BluetoothGattCharacteristic _characteristic) {
        if (_characteristic == null) {
            return false;
        }
        UUID uuid = _characteristic.getUuid();
        return matchCharacteristicUUID(uuid.toString());
    }

    /**
     * Query the given uuid as string to the provided characteristics by the server
     * @param _characteristic_uuid_string query uuid as string
     * @return true if the matched is found
     */
    private static boolean matchCharacteristicUUID(String _characteristic_uuid_string) {
        return matchUUIDs(_characteristic_uuid_string, CHARACTERISTIC_UUID);
    }

    /**
     * Try to match a uuid with the given set of uuid
     * @param _uuid_string uuid to query
     * @param _matches a set of uuid
     * @return true if matched
     */
    private static boolean matchUUIDs(String _uuid_string, String... _matches) {
        for (String match : _matches) {
            if (_uuid_string.equalsIgnoreCase(match)) {
                return true;
            }
        }

        return false;
    }
}

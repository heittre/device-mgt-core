/*
 * Copyright (c) 2018 - 2023, Entgra (Pvt) Ltd. (http://www.entgra.io) All Rights Reserved.
 *
 * Entgra (Pvt) Ltd. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.entgra.device.mgt.core.device.mgt.core.traccar.common.beans;

public class TraccarPosition {
    private int id;
    private String deviceIdentifier;
    private Long timestamp;
    private Double lat;
    private Double lon;
    private Float bearing;
    private Float speed;

    public TraccarPosition(int id, String deviceIdentifier, long timestamp, Double lat, Double lon,
                           float bearing, float speed){
        this.id =id;
        this.deviceIdentifier =deviceIdentifier;
        this.timestamp=timestamp;
        this.lat=lat;
        this.lon =lon;
        this.bearing =bearing;
        this.speed =speed;
    }

    public TraccarPosition(String deviceIdentifier, long timestamp, Double lat, Double lon,
                           float bearing, float speed){
        this.deviceIdentifier =deviceIdentifier;
        this.timestamp=timestamp;
        this.lat=lat;
        this.lon =lon;
        this.bearing =bearing;
        this.speed =speed;
    }

    public TraccarPosition(){ }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public void setDeviceIdentifier(String deviceIdentifier) {
        this.deviceIdentifier = deviceIdentifier;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public Float getBearing() {
        return bearing;
    }

    public void setBearing(Float bearing) {
        this.bearing = bearing;
    }

    public Float getSpeed() {
        return speed;
    }

    public void setSpeed(Float speed) {
        this.speed = speed;
    }
}

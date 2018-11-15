/*
 * Copyright sablintolya@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.ma1uta.jxclient.account;

import javafx.beans.property.SimpleStringProperty;

/**
 * Room.
 */
public class Room {

    private SimpleStringProperty topic = new SimpleStringProperty();

    public String getTopic() {
        return topic.getValue();
    }

    /**
     * Set a topic.
     *
     * @param topic the room topic.
     */
    public void setTopic(String topic) {
        this.topic.setValue(topic);
    }

    /**
     * Topic property.
     *
     * @return The topic property.
     */
    public SimpleStringProperty topicProperty() {
        return topic;
    }
}

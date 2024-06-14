package com.chat.shared;

import java.io.Serializable;

public record ChatData(int id, ChatType type , String publicName, String privateName) implements Serializable {
}

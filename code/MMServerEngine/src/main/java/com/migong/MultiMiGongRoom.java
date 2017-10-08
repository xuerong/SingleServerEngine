package com.migong;

import com.migong.entity.MatchingUser;
import com.migong.map.CreateMap;

import java.util.List;

public class MultiMiGongRoom extends MiGongRoom {
    private List<MatchingUser> matchingUsers;

    public MultiMiGongRoom(CreateMap createMap, List<MatchingUser> matchingUsers) {
        super(createMap);
        this.matchingUsers = matchingUsers;
    }
}

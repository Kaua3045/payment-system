package com.payment.system.infrastructure.frauds.blocklist;

import java.util.Set;

public interface BlockListSource {

    Set<String> load();
}

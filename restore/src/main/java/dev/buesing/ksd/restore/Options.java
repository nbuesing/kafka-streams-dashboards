package dev.buesing.ksd.restore;

import com.beust.jcommander.Parameter;
import dev.buesing.ksd.tools.config.BaseOptions;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Options extends BaseOptions {

    @Parameter(names = { "--changelog-topic" }, description = "")
    private String changelogTopic= "analytics_GRADLE-NONE-aggregate-purchase-order-changelog";

    @Parameter(names = { "--restore-topic" }, description = "")
    private String restoreTopic= "pickup-order-handler-purchase-order-join-product-repartition-restore";

    @Parameter(names = { "--group-id" }, description = "")
    private String groupId= "restore";


}

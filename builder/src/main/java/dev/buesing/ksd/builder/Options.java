package dev.buesing.ksd.builder;

import com.beust.jcommander.Parameter;
import dev.buesing.ksd.tools.config.BaseOptions;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Options extends BaseOptions {

    @Parameter(names = { "--delete-topics" }, description = "")
    private boolean deleteTopics = false;

}

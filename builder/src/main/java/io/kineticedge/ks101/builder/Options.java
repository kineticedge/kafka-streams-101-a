package io.kineticedge.ks101.builder;

import com.beust.jcommander.Parameter;
import io.kineticedge.ks101.tools.config.BaseOptions;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Options extends BaseOptions {

    @Parameter(names = { "--delete-topics" }, description = "")
    private boolean deleteTopics = false;

}

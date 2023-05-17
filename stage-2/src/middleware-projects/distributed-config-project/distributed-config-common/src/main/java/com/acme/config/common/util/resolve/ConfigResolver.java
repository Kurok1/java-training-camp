package com.acme.config.common.util.resolve;

import com.acme.config.common.ConfigKind;

import java.util.Map;
import java.util.function.Function;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
interface ConfigResolver extends Function<String, Map<String, String>> {

    ConfigKind supportKind();

}

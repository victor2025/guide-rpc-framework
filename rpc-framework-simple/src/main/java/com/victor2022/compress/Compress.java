package com.victor2022.compress;

import com.victor2022.extension.SPI;

/**
 * @author wangtao .
 * @createTime on 2020/10/3
 */

@SPI
public interface Compress {

    byte[] compress(byte[] bytes);


    byte[] decompress(byte[] bytes);
}

package cn.edu.ruc.iir.paraflow.examples.collector;

import cn.edu.ruc.iir.paraflow.commons.ParaflowFiberPartitioner;

import java.util.Objects;

/**
 * paraflow
 *
 * @author guodong
 */
public class BasicParaflowFiberPartitioner
        implements ParaflowFiberPartitioner
{
    @Override
    public int getFiberId(byte[] key)
    {
        return Objects.hashCode(key) % 10;
    }
}
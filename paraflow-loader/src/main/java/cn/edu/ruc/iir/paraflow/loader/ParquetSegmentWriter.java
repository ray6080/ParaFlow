package cn.edu.ruc.iir.paraflow.loader;

import cn.edu.ruc.iir.paraflow.metaserver.client.MetaClient;
import cn.edu.ruc.iir.paraflow.metaserver.proto.MetaProto;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.column.ParquetProperties;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.GroupFactory;
import org.apache.parquet.example.data.simple.SimpleGroupFactory;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.example.GroupWriteSupport;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.MessageTypeParser;

import java.io.IOException;

/**
 * paraflow
 *
 * @author guodong
 */
public class ParquetSegmentWriter
        extends SegmentWriter
{
    ParquetSegmentWriter(ParaflowSegment segment, int partitionFrom, int partitionTo, MetaClient metaClient)
    {
        super(segment, partitionFrom, partitionTo, metaClient);
    }

    @Override
    public boolean write(ParaflowSegment segment, MetaProto.StringListType columnNames, MetaProto.StringListType columnTypes)
    {
        // construct schema
        int columnNum = columnTypes.getStrCount();
        StringBuilder schemaBuilder = new StringBuilder("message " + segment.getTable() + " {");
        for (int i = 0; i < columnNum; i++) {
            switch (columnTypes.getStr(i)) {
                case "bigint":
                    schemaBuilder.append("required INT64 ").append(columnNames.getStr(i)).append("; ");
                    break;
                case "int":
                    schemaBuilder.append("required INT32 ").append(columnNames.getStr(i)).append("; ");
                    break;
                case "boolean":
                    schemaBuilder.append("required BOOLEAN ").append(columnNames.getStr(i)).append("; ");
                    break;
                case "float32":
                    schemaBuilder.append("required FLOAT ").append(columnNames.getStr(i)).append("; ");
                    break;
                case "float64":
                    schemaBuilder.append("required DOUBLE ").append(columnNames.getStr(i)).append("; ");
                    break;
                case "timestamp":
                    schemaBuilder.append("required INT64 ").append(columnNames.getStr(i)).append("; ");
                    break;
                case "real" :
                    schemaBuilder.append("required INT64 ").append(columnNames.getStr(i)).append("; ");
                    break;
                default:
                    schemaBuilder.append("required BINARY ").append(columnNames.getStr(i)).append("; ");
                    break;
            }
        }
        schemaBuilder.append("}");
        System.out.println("Schema: " + schemaBuilder.toString());
        MessageType schema = MessageTypeParser.parseMessageType(schemaBuilder.toString());
        GroupFactory groupFactory = new SimpleGroupFactory(schema);
        GroupWriteSupport writeSupport = new GroupWriteSupport();
        GroupWriteSupport.setSchema(schema, configuration);
        CompressionCodecName compressionCodecName;
        String compressionCodecStr = config.getParquetCompressionCodec();
        switch (compressionCodecStr.toLowerCase()) {
            case "gzip":
                compressionCodecName = CompressionCodecName.GZIP;
                break;
            case "lzo":
                compressionCodecName = CompressionCodecName.LZO;
                break;
            case "snappy":
                compressionCodecName = CompressionCodecName.SNAPPY;
                break;
            default:
                compressionCodecName = CompressionCodecName.UNCOMPRESSED;
                break;
        }
        try (ParquetWriter<Group> writer = new ParquetWriter<>(
                new Path(segment.getPath()), writeSupport, compressionCodecName,
                config.getParquetBlockSize(), config.getParquetPageSize(),
                config.getParquetDictionaryPageSize(), config.isParquetDictionaryEnabled(), config.isParquetValidating(),
                ParquetProperties.WriterVersion.PARQUET_2_0, configuration)) {
            ParaflowRecord[] content = segment.getRecords();
            for (ParaflowRecord record : content) {
                Group group = groupFactory.newGroup();
                for (int k = 0; k < columnNum; k++) {
                    switch (columnTypes.getStr(k)) {
                        case "bigint":
                            group.append(columnNames.getStr(k), (long) record.getValue(k));
                            break;
                        case "int":
                            group.append(columnNames.getStr(k), (int) record.getValue(k));
                            break;
                        case "boolean":
                            group.append(columnNames.getStr(k), (boolean) record.getValue(k));
                            break;
                        case "float32":
                            group.append(columnNames.getStr(k), (float) record.getValue(k));
                            break;
                        case "float64":
                            group.append(columnNames.getStr(k), (double) record.getValue(k));
                            break;
                        case "timestamp":
                            group.append(columnNames.getStr(k), (long) record.getValue(k));
                            break;
                        case "real":
                            group.append(columnNames.getStr(k), (long) record.getValue(k));
                            break;
                        default:
                            group.append(columnNames.getStr(k), (String) record.getValue(k));
                            break;
                    }
                }
                writer.write(group);
            }
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
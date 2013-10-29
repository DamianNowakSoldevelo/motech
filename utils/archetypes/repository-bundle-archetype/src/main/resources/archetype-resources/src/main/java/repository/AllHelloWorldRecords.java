#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.repository;

import ${package}.domain.HelloWorldRecord;

import java.util.List;

/**
 * Interface for the Hello World record repository, used for persisting records.
 */
public interface AllHelloWorldRecords {

    void add(HelloWorldRecord record);

    HelloWorldRecord findByRecordName(String recordName);

    List<HelloWorldRecord> getRecords();

    void delete(HelloWorldRecord record);
}

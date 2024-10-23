package dev.aulait.jeg.core.application;

import dev.aulait.jeg.core.domain.jdbc.DatabaseMetaDataModel;
import dev.aulait.jeg.core.domain.jdbc.DatabaseMetaDataProcessor;
import dev.aulait.jeg.core.domain.jdbc.DatabaseMetaDataReader;
import dev.aulait.jeg.core.domain.jdbc.TableModel;
import dev.aulait.jeg.core.domain.jpa.EmbeddedIdModel;
import dev.aulait.jeg.core.domain.jpa.EntityImportProcessor;
import dev.aulait.jeg.core.domain.jpa.EntityModel;
import dev.aulait.jeg.core.domain.jpa.RelationProcessor;
import dev.aulait.jeg.core.domain.jpa.RelationProcessorOneDirectionalImpl;
import dev.aulait.jeg.core.domain.jpa.TableToEntityProcessor;
import dev.aulait.jeg.core.infra.config.Config;
import dev.aulait.jeg.core.infra.formatter.FormatterFactory;
import dev.aulait.jeg.core.infra.template.TemplateProcessor;
import dev.aulait.jeg.core.infra.textfile.TextFileModel;
import dev.aulait.jeg.core.infra.textfile.TextFileWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class EntityGenerator {

  DatabaseMetaDataReader reader;
  TableToEntityProcessor tableToEntityProcessor;
  DatabaseMetaDataProcessor databaseMetaDataProcessor = new DatabaseMetaDataProcessor();
  RelationProcessor relationProcessor;
  EntityImportProcessor entityImportProcessor = new EntityImportProcessor();
  TemplateProcessor templateProcessor = new TemplateProcessor();
  TextFileWriter writer = new TextFileWriter();

  public EntityGenerator(Config config) {
    reader = new DatabaseMetaDataReader(config);
    tableToEntityProcessor = new TableToEntityProcessor(config);
    relationProcessor = new RelationProcessorOneDirectionalImpl(config);
    templateProcessor.setFormatter(FormatterFactory.create(config.getFormatter()));
  }

  public List<Path> execute() {
    DatabaseMetaDataModel meta = reader.read();
    List<TableModel> tables = databaseMetaDataProcessor.process(meta);

    List<EntityModel> entities = tableToEntityProcessor.process(tables);

    relationProcessor.process(tables, entities);

    entityImportProcessor.process(entities);

    List<TextFileModel> files = templateProcessor.process(entities);

    List<EmbeddedIdModel> embeddedIds =
        entities.stream().map(EntityModel::getEmbeddedId).filter(Objects::nonNull).toList();

    files.addAll(templateProcessor.process(embeddedIds));

    return writer.write(files);
  }
}

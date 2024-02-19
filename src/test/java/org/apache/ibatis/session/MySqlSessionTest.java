package org.apache.ibatis.session;

import java.io.Reader;
import javax.sql.DataSource;
import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.domain.blog.Author;
import org.apache.ibatis.domain.blog.mappers.AuthorMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MySqlSessionTest extends BaseDataTest {
  private static SqlSessionFactory sqlSessionFactory;

  @BeforeAll
  static void setup() throws Exception {
    DataSource dataSource = createBlogDataSource();

    // case1 使用xml配置Configuration
//    final String resource = "org/apache/ibatis/builder/MapperConfig.xml";
//    final Reader reader = Resources.getResourceAsReader(resource);
//    sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);

    // case2 编码配置Configuration
    TransactionFactory transactionFactory = new JdbcTransactionFactory();
    // 数据源 + 事务管理器 -> Environment
    Environment environment = new Environment("development", transactionFactory, dataSource);
    // Environment + Mapper -> Configuration
    Configuration configuration = new Configuration(environment);
    configuration.addMapper(AnnotationAuthorMapper.class);
    // Configuration -> SqlSessionFactory
    sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
  }

  public interface AnnotationAuthorMapper {
    @Select("select id,username from author where id = #{id}")
    Author selectAuthor(int id);
  }

  @Test
  void test() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      AnnotationAuthorMapper mapper = sqlSession.getMapper(AnnotationAuthorMapper.class);
      System.out.println(mapper.selectAuthor(101));
    }
  }
}

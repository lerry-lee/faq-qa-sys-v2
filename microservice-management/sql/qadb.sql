-- MySQL dump 10.13  Distrib 8.0.25, for Linux (x86_64)
--
-- Host: localhost    Database: qadb
-- ------------------------------------------------------
-- Server version	8.0.25-0ubuntu0.20.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `feedback`
--

DROP TABLE IF EXISTS `feedback`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `feedback` (
  `id` int NOT NULL AUTO_INCREMENT,
  `question` text NOT NULL,
  `type` varchar(50) NOT NULL,
  `reason` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `feedback`
--

LOCK TABLES `feedback` WRITE;
/*!40000 ALTER TABLE `feedback` DISABLE KEYS */;
/*!40000 ALTER TABLE `feedback` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `history`
--

DROP TABLE IF EXISTS `history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `history` (
  `id` int NOT NULL AUTO_INCREMENT,
  `msg_id` varchar(50) NOT NULL,
  `type` varchar(50) NOT NULL,
  `content_text` text,
  `position` varchar(50) NOT NULL,
  `created_at` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `history`
--

LOCK TABLES `history` WRITE;
/*!40000 ALTER TABLE `history` DISABLE KEYS */;
/*!40000 ALTER TABLE `history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stdq_simq`
--

DROP TABLE IF EXISTS `stdq_simq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stdq_simq` (
  `id` int NOT NULL AUTO_INCREMENT,
  `qa_id` int NOT NULL COMMENT '标准问的唯一标识，用于多表数据关联一致性',
  `standard_question` text NOT NULL COMMENT '标准问',
  `similar_question` text NOT NULL COMMENT '相似问',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stdq_simq`
--

LOCK TABLES `stdq_simq` WRITE;
/*!40000 ALTER TABLE `stdq_simq` DISABLE KEYS */;
INSERT INTO `stdq_simq` VALUES (1,1,'你好','你好'),(2,2,'你会什么功能','你会什么功能'),(3,3,'推荐一个景点','推荐一个景点'),(4,1,'你好','哈楼'),(5,1,'你好','hello'),(6,1,'你好','嗨'),(7,2,'你会什么功能','你有什么作用'),(8,2,'你会什么功能','你能干什么'),(9,3,'推荐一个景点','推荐个旅游景点');
/*!40000 ALTER TABLE `stdq_simq` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stdq_stda`
--

DROP TABLE IF EXISTS `stdq_stda`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stdq_stda` (
  `id` int NOT NULL AUTO_INCREMENT,
  `qa_id` int NOT NULL COMMENT '标准问-标准答的唯一标识id，确保多表数据关联一致性',
  `standard_question` text NOT NULL COMMENT '标准问',
  `category1` varchar(255) NOT NULL COMMENT '一级类别',
  `category2` varchar(255) NOT NULL COMMENT '二级类别',
  `standard_answer` text NOT NULL COMMENT '标准答',
  PRIMARY KEY (`id`),
  UNIQUE KEY `qaId` (`qa_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stdq_stda`
--

LOCK TABLES `stdq_stda` WRITE;
/*!40000 ALTER TABLE `stdq_stda` DISABLE KEYS */;
INSERT INTO `stdq_stda` VALUES (4,1,'你好','寒暄','打招呼','你好呀，我是智能机器人小木，请问有什么问题～'),(5,2,'你会什么功能','寒暄','询问','我会的功能可多了，比如可以给你推荐景点'),(6,3,'推荐一个景点','推荐','旅游','多轮');
/*!40000 ALTER TABLE `stdq_stda` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `unknown_question`
--

DROP TABLE IF EXISTS `unknown_question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `unknown_question` (
  `id` int NOT NULL AUTO_INCREMENT,
  `question` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `unknown_question`
--

LOCK TABLES `unknown_question` WRITE;
/*!40000 ALTER TABLE `unknown_question` DISABLE KEYS */;
INSERT INTO `unknown_question` VALUES (8,'哈哈哈哈'),(9,'哈哈'),(10,'哈哈'),(11,'哈哈'),(12,'哈哈'),(13,'哈哈'),(14,'哈哈'),(15,'哈哈'),(16,'哈哈'),(17,'哈哈'),(18,'哈哈'),(19,'哈哈'),(20,'哈哈'),(21,'哈哈'),(22,'吕布'),(23,'张良'),(24,'haha '),(25,'哈哈'),(26,'哈哈'),(27,'哈哈'),(28,'哈哈'),(29,'哈哈'),(30,'哈哈'),(31,'哈哈'),(32,'吕布'),(33,'哈哈');
/*!40000 ALTER TABLE `unknown_question` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2022-06-01 23:20:54

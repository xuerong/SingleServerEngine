/*
Navicat MySQL Data Transfer

Source Server         : 10.1.6.254
Source Server Version : 50711
Source Host           : 10.1.6.254:3306
Source Database       : test

Target Server Type    : MYSQL
Target Server Version : 50711
File Encoding         : 65001

Date: 2017-11-20 14:09:58
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for pvpRecord
-- ----------------------------
DROP TABLE IF EXISTS `pvpRecord`;
CREATE TABLE `pvpRecord` (
  `id` bigint(20) NOT NULL,
  `time` timestamp NULL DEFAULT NULL,
  `grade` int(11) DEFAULT NULL,
  `record` mediumtext,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

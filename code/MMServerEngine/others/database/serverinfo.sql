/*
Navicat MySQL Data Transfer

Source Server         : 10.1.6.254
Source Server Version : 50711
Source Host           : 10.1.6.254:3306
Source Database       : test

Target Server Type    : MYSQL
Target Server Version : 50711
File Encoding         : 65001

Date: 2017-11-20 14:10:13
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for serverInfo
-- ----------------------------
DROP TABLE IF EXISTS `serverInfo`;
CREATE TABLE `serverInfo` (
  `id` int(11) NOT NULL,
  `ip` varchar(255) NOT NULL,
  `port` int(11) NOT NULL,
  `accountCount` int(11) NOT NULL,
  `hot` int(11) NOT NULL,
  `state` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of serverinfo
-- ----------------------------
INSERT INTO `serverinfo` VALUES ('1', '127.0.0.1', '8003', '0', '0','0');

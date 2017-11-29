/*
Navicat MySQL Data Transfer

Source Server         : 10.1.6.254
Source Server Version : 50711
Source Host           : 10.1.6.254:3306
Source Database       : test

Target Server Type    : MYSQL
Target Server Version : 50711
File Encoding         : 65001

Date: 2017-11-20 14:10:48
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for userPass
-- ----------------------------
DROP TABLE IF EXISTS `userPass`;
CREATE TABLE `userPass` (
  `userId` varchar(255) NOT NULL,
  `passId` int(11) NOT NULL,
  `star` int(11) NOT NULL DEFAULT '0',
  `useTime` int(11) NOT NULL DEFAULT '0',
  `score` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`userId`,`passId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

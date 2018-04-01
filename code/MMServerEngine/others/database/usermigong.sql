/*
Navicat MySQL Data Transfer

Source Server         : 10.1.6.254
Source Server Version : 50711
Source Host           : 10.1.6.254:3306
Source Database       : test

Target Server Type    : MYSQL
Target Server Version : 50711
File Encoding         : 65001

Date: 2017-11-20 14:10:38
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for userMiGong
-- ----------------------------
DROP TABLE IF EXISTS `userMiGong`;
CREATE TABLE `userMiGong` (
  `userId` varchar(255) NOT NULL,
  `unlimitedPass` int(11) NOT NULL DEFAULT '0',
  `unlimitedStar` int(11) NOT NULL DEFAULT '0',
  `pass` int(11) NOT NULL DEFAULT '0',
  `starCount` int(11) NOT NULL DEFAULT '0',
  `vip` int(11) NOT NULL DEFAULT '0',
  `pvpTimes` int(11) NOT NULL DEFAULT '0',
  `ladderScore` int(11) NOT NULL DEFAULT '0',
  `energy` int(11) NOT NULL DEFAULT '0',
  `energyUpdateTime` bigint(20) NOT NULL DEFAULT '0',
  `newUserGuide` varchar(255) DEFAULT NULL,
  `gold` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

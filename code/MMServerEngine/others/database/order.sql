/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 50719
 Source Host           : localhost
 Source Database       : test

 Target Server Type    : MySQL
 Target Server Version : 50719
 File Encoding         : utf-8

 Date: 12/12/2017 23:08:33 PM
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `order`
-- ----------------------------
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order` (
  `id` int(11) NOT NULL,
  `token` varchar(255) DEFAULT NULL,
  `userId` varchar(255) DEFAULT NULL,
  `peckId` int(11) DEFAULT NULL,
  `num` int(11) DEFAULT NULL,
  `time` timestamp NULL DEFAULT NULL,
  `success` int(11) DEFAULT NULL,
  `money` int(11) DEFAULT NULL,
  `gold` int(11) DEFAULT NULL,
  `items` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

SET FOREIGN_KEY_CHECKS = 1;

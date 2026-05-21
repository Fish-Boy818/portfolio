#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
账户管理器模块，自动读取和处理抖音账户ID
"""

import os
import re
import json
from datetime import datetime

class AccountManager:
    """账户管理器类，用于自动读取和管理抖音账户ID"""
    
    def __init__(self):
        """初始化账户管理器"""
        self.accounts_data = {}
        self.load_default_accounts()
    
    def load_default_accounts(self):
        """加载默认的账户ID和名称"""
        # Public repository default data must stay generic. Put real account data
        # in account_names.txt locally, or start from account_names.example.txt.
        default_accounts = {
            "1234567890123": "示例账号A",
            "2345678901234": "示例账号B",
        }
        
        self.accounts_data = default_accounts
        print(f"已加载 {len(default_accounts)} 个默认账户")
    
    def load_from_file(self, file_path):
        """从文件加载账户数据"""
        if not os.path.exists(file_path):
            print(f"文件不存在: {file_path}")
            return False
        
        try:
            # 判断文件类型
            if file_path.endswith('.json'):
                # JSON 文件
                with open(file_path, 'r', encoding='utf-8') as f:
                    self.accounts_data = json.load(f)
            else:
                # 普通文本文件
                accounts = {}
                with open(file_path, 'r', encoding='utf-8') as f:
                    for line in f:
                        line = line.strip()
                        if not line:
                            continue
                            
                        # 检查是否包含分隔符（如冒号）
                        if ":" in line:
                            account_id, account_name = line.split(":", 1)
                            accounts[account_id.strip()] = account_name.strip()
                        else:
                            # 假设只有账户ID
                            account_id = line
                            accounts[account_id] = f"账户_{account_id}"
                
                self.accounts_data = accounts
                
            print(f"已从文件 '{file_path}' 加载 {len(self.accounts_data)} 个账户")
            return True
        except Exception as e:
            print(f"从文件加载账户数据失败: {str(e)}")
            return False
    
    def save_to_file(self, file_path):
        """保存账户数据到文件"""
        try:
            # 根据文件扩展名选择保存格式
            if file_path.endswith('.json'):
                # JSON 格式
                with open(file_path, 'w', encoding='utf-8') as f:
                    json.dump(self.accounts_data, f, ensure_ascii=False, indent=2)
            else:
                # 普通文本格式
                with open(file_path, 'w', encoding='utf-8') as f:
                    for account_id, account_name in self.accounts_data.items():
                        f.write(f"{account_id}: {account_name}\n")
                        
            print(f"已将 {len(self.accounts_data)} 个账户保存到文件 '{file_path}'")
            return True
        except Exception as e:
            print(f"保存账户数据到文件失败: {str(e)}")
            return False
    
    def add_account(self, account_id, account_name):
        """添加或更新账户"""
        self.accounts_data[account_id] = account_name
        print(f"已添加/更新账户: {account_id} - {account_name}")
    
    def remove_account(self, account_id):
        """移除账户"""
        if account_id in self.accounts_data:
            account_name = self.accounts_data.pop(account_id)
            print(f"已移除账户: {account_id} - {account_name}")
            return True
        else:
            print(f"账户不存在: {account_id}")
            return False
    
    def get_account_name(self, account_id):
        """获取账户名称"""
        return self.accounts_data.get(account_id, f"未命名账户_{account_id}")
    
    def get_all_account_ids(self):
        """获取所有账户ID"""
        return list(self.accounts_data.keys())
    
    def extract_account_id_from_text(self, text):
        """从文本中提取可能的抖音账户ID（通常是13位数字）"""
        if not text:
            return None
        
        # 查找常见的ID模式
        # 1. ID: 后跟数字
        id_patterns = [
            r'ID[：:]\s*(\d{13})',  # ID: 后跟13位数字
            r'ID[：:]\s*([0-9a-zA-Z]+)', # ID: 后跟任意数字字母组合
            r'(\d{13})'  # 任何13位数字
        ]
        
        for pattern in id_patterns:
            matches = re.findall(pattern, text)
            if matches:
                # 返回第一个匹配的ID
                return matches[0]
        
        return None
    
    def auto_extract_ids_from_html(self, html_content):
        """从HTML内容中自动提取账户ID和名称"""
        if not html_content:
            return []
        
        extracted_accounts = []
        
        # 尝试识别常见的ID和名称模式
        # 1. 包含ID的span元素
        id_spans = re.findall(r'<span[^>]*>ID[：:]\s*(\d{13})[^<]*</span>', html_content)
        
        # 2. 包含账户名称的元素通常在ID附近
        name_patterns = [
            r'<[^>]*class="[^"]*name[^"]*"[^>]*>([^<]+)</[^>]+>',
            r'<[^>]*class="[^"]*title[^"]*"[^>]*>([^<]+)</[^>]+>',
            r'<h[1-6][^>]*>([^<]+)</h[1-6]>'
        ]
        
        # 提取所有可能的名称
        potential_names = []
        for pattern in name_patterns:
            matches = re.findall(pattern, html_content)
            potential_names.extend(matches)
        
        # 将ID和可能的名称配对
        for i, account_id in enumerate(id_spans):
            account_name = "未命名账户"
            # 如果有对应索引的名称，使用它
            if i < len(potential_names):
                account_name = potential_names[i].strip()
            
            if account_id and account_name:
                extracted_accounts.append((account_id, account_name))
                # 同时更新账户数据
                self.add_account(account_id, account_name)
        
        print(f"从HTML内容中提取了 {len(extracted_accounts)} 个账户信息")
        return extracted_accounts
    
    def auto_extract_ids_from_browser(self, driver):
        """从浏览器中自动提取账户ID和名称"""
        print("开始从浏览器中提取账户信息...")
        extracted_accounts = []
        
        try:
            # 尝试获取页面源代码
            html_content = driver.page_source
            
            # 1. 尝试从HTML内容中提取
            html_extracted = self.auto_extract_ids_from_html(html_content)
            if html_extracted:
                extracted_accounts.extend(html_extracted)
            
            # 2. 尝试从DOM元素中提取
            try:
                # 查找可能包含账户信息的元素
                account_elements = driver.find_elements("css selector", 
                    ".account-card, .box-account-item, .account-item, [class*='account-item'], [class*='account_item']")
                
                for element in account_elements:
                    try:
                        # 获取元素文本
                        text = element.text
                        if not text:
                            continue
                        
                        # 提取ID
                        account_id = self.extract_account_id_from_text(text)
                        if not account_id:
                            continue
                        
                        # 提取名称 - 取第一行作为可能的名称
                        lines = text.split('\n')
                        account_name = lines[0].strip() if lines else "未命名账户"
                        
                        # 如果第一行包含ID，尝试使用其他行
                        if "ID" in account_name:
                            for line in lines:
                                if "ID" not in line:
                                    account_name = line.strip()
                                    break
                        
                        if account_id and account_name:
                            extracted_accounts.append((account_id, account_name))
                            # 同时更新账户数据
                            self.add_account(account_id, account_name)
                    except Exception as e:
                        print(f"提取账户元素信息时出错: {str(e)}")
            except Exception as e:
                print(f"从DOM提取账户信息时出错: {str(e)}")
            
            print(f"从浏览器中成功提取 {len(extracted_accounts)} 个账户信息")
            return extracted_accounts
            
        except Exception as e:
            print(f"从浏览器提取账户信息时发生错误: {str(e)}")
            return []
    
    def generate_report(self):
        """生成账户报告"""
        report = "抖音账户列表报告\n"
        report += f"生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n"
        report += f"账户总数: {len(self.accounts_data)}\n\n"
        
        for i, (account_id, account_name) in enumerate(self.accounts_data.items(), 1):
            report += f"{i}. ID: {account_id} - 名称: {account_name}\n"
        
        return report

# 如果直接运行此模块，执行简单的测试
if __name__ == "__main__":
    # 创建账户管理器实例
    manager = AccountManager()
    
    # 显示默认账户
    print("\n默认账户列表:")
    for i, (account_id, account_name) in enumerate(manager.accounts_data.items(), 1):
        print(f"{i}. {account_id} - {account_name}")
    
    # 保存账户列表到文件
    manager.save_to_file("auto_accounts.json")
    
    print("\n生成报告:")
    print(manager.generate_report()) 


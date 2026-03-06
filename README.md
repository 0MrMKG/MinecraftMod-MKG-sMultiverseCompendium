2026-3-4 update v0.1:
    制定了nbt标准命名规则：

```
【全局唯一的建筑】                 [unique] (限制全局/区块生成数量)

【建筑封口层】      [roof] (屋顶) ── [branch] (依附物)           
                    ▲                                
                    │              
                    │                                
【标准楼层】        [upper_floor] (第二层及以上房屋) ──┬── [joint_we/sn] (转向枢纽) ── [Bridge] (桥梁/栈道)
                    ▲                                │
                    │                                └── [platform] (随机生成的平台) ── [branch] (依附物)
                    │
                    │ 
                    │
【底层锚点】        [root] (第一层房屋主体) ─────────┬─── [path] (街道)
                    ▲                              │
                    │                              └── [root_platform] (底层平台/广场) ── [branch] (依附物)
                    │ 
                    │ 
【地基】          [base] 
```

_base_
_root_ _path_  _root_platform_   
_upper_floor_  _joint_we_ _joint_sn_ _bridge_ _branch_
_roof_
_unique_

修改后的字段
test_base_1
test_root_1
test_upper_floor_1
test_roof_1


test_path_1

test_root_platform_1

test_joint_we_1
test_joint_sn_1
test_bridge_1
test_branch_1

test_unique_1

m
server_savepoint/Const:0*save_server_savepoint/control_dependency:0server_savepoint/restore_all 5 @F8*initialize_server_state_and_non_state_vars"л
"^
data_token:0
input_filepath:0 
output_filepath:0 "save_client_update_tensorsК> *%
input_filepath:0output_filepath:0Ю
group_deps_4
h
client_savepoint/Const:0%client_savepoint/control_dependency:0client_savepoint/restore_all 5 @F8"group_deps_2:write_client_session_token:0
h
update_savepoint/Const:0%update_savepoint/control_dependency:0update_savepoint/restore_all 5 @F8initialize_update_vars"group_deps_3*group_deps_62!
Identity_34:0server/eval/loss:

%intermediate_update_savepoint/Const:02intermediate_update_savepoint/control_dependency:0)intermediate_update_savepoint/restore_all 5 @F8J

%intermediate_update_savepoint/Const:02intermediate_update_savepoint/control_dependency:0)intermediate_update_savepoint/restore_all 5 @F8R
group_depsgroup_deps_1Zgroup_deps_5b

#aggregated_update_savepoint/Const:00aggregated_update_savepoint/control_dependency:0'aggregated_update_savepoint/restore_all 5 @F8:НЉ
'type.googleapis.com/tensorflow.GraphDefЉ
p
;server/global_model_weights/trainable/zeros/shape_as_tensorConst*
dtype0*
valueB"     
^
1server/global_model_weights/trainable/zeros/ConstConst*
dtype0*
valueB
 *    
О
+server/global_model_weights/trainable/zerosFill;server/global_model_weights/trainable/zeros/shape_as_tensor1server/global_model_weights/trainable/zeros/Const*

index_type0*
T0
ј
'server/global_model_weights/trainable/0VarHandleOp*
dtype0*
	container *
shape:
*8
shared_name)'server/global_model_weights/trainable/0*
allowed_devices
 *:
_class0
.,loc:@server/global_model_weights/trainable/0

Hserver/global_model_weights/trainable/0/IsInitialized/VarIsInitializedOpVarIsInitializedOp'server/global_model_weights/trainable/0
у
.server/global_model_weights/trainable/0/AssignAssignVariableOp'server/global_model_weights/trainable/0+server/global_model_weights/trainable/zeros*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(

;server/global_model_weights/trainable/0/Read/ReadVariableOpReadVariableOp'server/global_model_weights/trainable/0*
dtype0
_
-server/global_model_weights/trainable/zeros_1Const*
dtype0*
valueB*    
ѓ
'server/global_model_weights/trainable/1VarHandleOp*:
_class0
.,loc:@server/global_model_weights/trainable/1*
	container *
shape:*8
shared_name)'server/global_model_weights/trainable/1*
allowed_devices
 *
dtype0

Hserver/global_model_weights/trainable/1/IsInitialized/VarIsInitializedOpVarIsInitializedOp'server/global_model_weights/trainable/1
х
.server/global_model_weights/trainable/1/AssignAssignVariableOp'server/global_model_weights/trainable/1-server/global_model_weights/trainable/zeros_1*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(

;server/global_model_weights/trainable/1/Read/ReadVariableOpReadVariableOp'server/global_model_weights/trainable/1*
dtype0
c
-server/global_model_weights/trainable/zeros_2Const*
dtype0*
valueB	*    
ї
'server/global_model_weights/trainable/2VarHandleOp*
dtype0*
	container *
shape:	*8
shared_name)'server/global_model_weights/trainable/2*
allowed_devices
 *:
_class0
.,loc:@server/global_model_weights/trainable/2

Hserver/global_model_weights/trainable/2/IsInitialized/VarIsInitializedOpVarIsInitializedOp'server/global_model_weights/trainable/2
х
.server/global_model_weights/trainable/2/AssignAssignVariableOp'server/global_model_weights/trainable/2-server/global_model_weights/trainable/zeros_2*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(

;server/global_model_weights/trainable/2/Read/ReadVariableOpReadVariableOp'server/global_model_weights/trainable/2*
dtype0
^
-server/global_model_weights/trainable/zeros_3Const*
dtype0*
valueB*    
ђ
'server/global_model_weights/trainable/3VarHandleOp*:
_class0
.,loc:@server/global_model_weights/trainable/3*
	container *
shape:*8
shared_name)'server/global_model_weights/trainable/3*
allowed_devices
 *
dtype0

Hserver/global_model_weights/trainable/3/IsInitialized/VarIsInitializedOpVarIsInitializedOp'server/global_model_weights/trainable/3
х
.server/global_model_weights/trainable/3/AssignAssignVariableOp'server/global_model_weights/trainable/3-server/global_model_weights/trainable/zeros_3*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(

;server/global_model_weights/trainable/3/Read/ReadVariableOpReadVariableOp'server/global_model_weights/trainable/3*
dtype0
`
3server/aggregator/query_state/numerator_state/zerosConst*
dtype0*
valueB
 *    
Ї
:server/aggregator/query_state/numerator_state/l2_norm_clipVarHandleOp*
dtype0*
	container *
shape: *K
shared_name<:server/aggregator/query_state/numerator_state/l2_norm_clip*
allowed_devices
 *M
_classC
A?loc:@server/aggregator/query_state/numerator_state/l2_norm_clip
­
[server/aggregator/query_state/numerator_state/l2_norm_clip/IsInitialized/VarIsInitializedOpVarIsInitializedOp:server/aggregator/query_state/numerator_state/l2_norm_clip

Aserver/aggregator/query_state/numerator_state/l2_norm_clip/AssignAssignVariableOp:server/aggregator/query_state/numerator_state/l2_norm_clip3server/aggregator/query_state/numerator_state/zeros*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
Љ
Nserver/aggregator/query_state/numerator_state/l2_norm_clip/Read/ReadVariableOpReadVariableOp:server/aggregator/query_state/numerator_state/l2_norm_clip*
dtype0
b
5server/aggregator/query_state/numerator_state/zeros_1Const*
dtype0*
valueB
 *    

4server/aggregator/query_state/numerator_state/stddevVarHandleOp*
dtype0*
	container *
shape: *E
shared_name64server/aggregator/query_state/numerator_state/stddev*
allowed_devices
 *G
_class=
;9loc:@server/aggregator/query_state/numerator_state/stddev
Ё
Userver/aggregator/query_state/numerator_state/stddev/IsInitialized/VarIsInitializedOpVarIsInitializedOp4server/aggregator/query_state/numerator_state/stddev

;server/aggregator/query_state/numerator_state/stddev/AssignAssignVariableOp4server/aggregator/query_state/numerator_state/stddev5server/aggregator/query_state/numerator_state/zeros_1*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(

Hserver/aggregator/query_state/numerator_state/stddev/Read/ReadVariableOpReadVariableOp4server/aggregator/query_state/numerator_state/stddev*
dtype0
P
#server/aggregator/query_state/zerosConst*
dtype0*
valueB
 *    
є
)server/aggregator/query_state/denominatorVarHandleOp*<
_class2
0.loc:@server/aggregator/query_state/denominator*
	container *
shape: *:
shared_name+)server/aggregator/query_state/denominator*
allowed_devices
 *
dtype0

Jserver/aggregator/query_state/denominator/IsInitialized/VarIsInitializedOpVarIsInitializedOp)server/aggregator/query_state/denominator
п
0server/aggregator/query_state/denominator/AssignAssignVariableOp)server/aggregator/query_state/denominator#server/aggregator/query_state/zeros*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(

=server/aggregator/query_state/denominator/Read/ReadVariableOpReadVariableOp)server/aggregator/query_state/denominator*
dtype0
I
 server/aggregator/dp_event/zerosConst*
dtype0*
valueB B 
ы
&server/aggregator/dp_event/module_nameVarHandleOp*
dtype0*
	container *
shape: *7
shared_name(&server/aggregator/dp_event/module_name*
allowed_devices
 *9
_class/
-+loc:@server/aggregator/dp_event/module_name

Gserver/aggregator/dp_event/module_name/IsInitialized/VarIsInitializedOpVarIsInitializedOp&server/aggregator/dp_event/module_name
ж
-server/aggregator/dp_event/module_name/AssignAssignVariableOp&server/aggregator/dp_event/module_name server/aggregator/dp_event/zeros*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(

:server/aggregator/dp_event/module_name/Read/ReadVariableOpReadVariableOp&server/aggregator/dp_event/module_name*
dtype0
K
"server/aggregator/dp_event/zeros_1Const*
dtype0*
valueB B 
ш
%server/aggregator/dp_event/class_nameVarHandleOp*8
_class.
,*loc:@server/aggregator/dp_event/class_name*
	container *
shape: *6
shared_name'%server/aggregator/dp_event/class_name*
allowed_devices
 *
dtype0

Fserver/aggregator/dp_event/class_name/IsInitialized/VarIsInitializedOpVarIsInitializedOp%server/aggregator/dp_event/class_name
ж
,server/aggregator/dp_event/class_name/AssignAssignVariableOp%server/aggregator/dp_event/class_name"server/aggregator/dp_event/zeros_1*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(

9server/aggregator/dp_event/class_name/Read/ReadVariableOpReadVariableOp%server/aggregator/dp_event/class_name*
dtype0
O
"server/aggregator/dp_event/zeros_2Const*
dtype0*
valueB
 *    
њ
+server/aggregator/dp_event/noise_multiplierVarHandleOp*>
_class4
20loc:@server/aggregator/dp_event/noise_multiplier*
	container *
shape: *<
shared_name-+server/aggregator/dp_event/noise_multiplier*
allowed_devices
 *
dtype0

Lserver/aggregator/dp_event/noise_multiplier/IsInitialized/VarIsInitializedOpVarIsInitializedOp+server/aggregator/dp_event/noise_multiplier
т
2server/aggregator/dp_event/noise_multiplier/AssignAssignVariableOp+server/aggregator/dp_event/noise_multiplier"server/aggregator/dp_event/zeros_2*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(

?server/aggregator/dp_event/noise_multiplier/Read/ReadVariableOpReadVariableOp+server/aggregator/dp_event/noise_multiplier*
dtype0
A
server/aggregator/zerosConst*
dtype0
*
value	B
 Z 
ж
server/aggregator/is_init_stateVarHandleOp*2
_class(
&$loc:@server/aggregator/is_init_state*
	container *
shape: *0
shared_name!server/aggregator/is_init_state*
allowed_devices
 *
dtype0

w
@server/aggregator/is_init_state/IsInitialized/VarIsInitializedOpVarIsInitializedOpserver/aggregator/is_init_state
П
&server/aggregator/is_init_state/AssignAssignVariableOpserver/aggregator/is_init_stateserver/aggregator/zeros*
dtype0
*
validate_shape( *&
 _has_manual_control_dependencies(
s
3server/aggregator/is_init_state/Read/ReadVariableOpReadVariableOpserver/aggregator/is_init_state*
dtype0

C
server/finalizer/zerosConst*
dtype0*
valueB
 *    
г
server/finalizer/learning_rateVarHandleOp*1
_class'
%#loc:@server/finalizer/learning_rate*
	container *
shape: */
shared_name server/finalizer/learning_rate*
allowed_devices
 *
dtype0
u
?server/finalizer/learning_rate/IsInitialized/VarIsInitializedOpVarIsInitializedOpserver/finalizer/learning_rate
М
%server/finalizer/learning_rate/AssignAssignVariableOpserver/finalizer/learning_rateserver/finalizer/zeros*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
q
2server/finalizer/learning_rate/Read/ReadVariableOpReadVariableOpserver/finalizer/learning_rate*
dtype0
?
metrics/eval/zerosConst*
dtype0*
valueB
 *    
Ќ
metrics/eval/lossVarHandleOp*$
_class
loc:@metrics/eval/loss*
	container *
shape: *"
shared_namemetrics/eval/loss*
allowed_devices
 *
dtype0
[
2metrics/eval/loss/IsInitialized/VarIsInitializedOpVarIsInitializedOpmetrics/eval/loss

metrics/eval/loss/AssignAssignVariableOpmetrics/eval/lossmetrics/eval/zeros*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
W
%metrics/eval/loss/Read/ReadVariableOpReadVariableOpmetrics/eval/loss*
dtype0
R
$save_server_savepoint/filename/inputConst*
dtype0*
valueB Bmodel
x
save_server_savepoint/filenamePlaceholderWithDefault$save_server_savepoint/filename/input*
dtype0*
shape: 
o
save_server_savepoint/ConstPlaceholderWithDefaultsave_server_savepoint/filename*
dtype0*
shape: 
ц
'save_server_savepoint/save/tensor_namesConst*
dtype0*І
valueBBmetrics/eval/lossB%server/aggregator/dp_event/class_nameB&server/aggregator/dp_event/module_nameB+server/aggregator/dp_event/noise_multiplierBserver/aggregator/is_init_stateB)server/aggregator/query_state/denominatorB:server/aggregator/query_state/numerator_state/l2_norm_clipB4server/aggregator/query_state/numerator_state/stddevBserver/finalizer/learning_rateB'server/global_model_weights/trainable/0B'server/global_model_weights/trainable/1B'server/global_model_weights/trainable/2B'server/global_model_weights/trainable/3
q
,save_server_savepoint/save/shapes_and_slicesConst*
dtype0*-
value$B"B B B B B B B B B B B B B 
ь
save_server_savepoint/save
SaveSlicesserver_savepoint/Const'save_server_savepoint/save/tensor_names,save_server_savepoint/save/shapes_and_slices%metrics/eval/loss/Read/ReadVariableOp9server/aggregator/dp_event/class_name/Read/ReadVariableOp:server/aggregator/dp_event/module_name/Read/ReadVariableOp?server/aggregator/dp_event/noise_multiplier/Read/ReadVariableOp3server/aggregator/is_init_state/Read/ReadVariableOp=server/aggregator/query_state/denominator/Read/ReadVariableOpNserver/aggregator/query_state/numerator_state/l2_norm_clip/Read/ReadVariableOpHserver/aggregator/query_state/numerator_state/stddev/Read/ReadVariableOp2server/finalizer/learning_rate/Read/ReadVariableOp;server/global_model_weights/trainable/0/Read/ReadVariableOp;server/global_model_weights/trainable/1/Read/ReadVariableOp;server/global_model_weights/trainable/2/Read/ReadVariableOp;server/global_model_weights/trainable/3/Read/ReadVariableOp*
T
2
*&
 _has_manual_control_dependencies(
Ђ
(save_server_savepoint/control_dependencyIdentityserver_savepoint/Const^save_server_savepoint/save*.
_class$
" loc:@save_server_savepoint/Const*
T0
њ
,save_server_savepoint/RestoreV2/tensor_namesConst"/device:CPU:0*
dtype0*І
valueBBmetrics/eval/lossB%server/aggregator/dp_event/class_nameB&server/aggregator/dp_event/module_nameB+server/aggregator/dp_event/noise_multiplierBserver/aggregator/is_init_stateB)server/aggregator/query_state/denominatorB:server/aggregator/query_state/numerator_state/l2_norm_clipB4server/aggregator/query_state/numerator_state/stddevBserver/finalizer/learning_rateB'server/global_model_weights/trainable/0B'server/global_model_weights/trainable/1B'server/global_model_weights/trainable/2B'server/global_model_weights/trainable/3

0save_server_savepoint/RestoreV2/shape_and_slicesConst"/device:CPU:0*
dtype0*-
value$B"B B B B B B B B B B B B B 
а
save_server_savepoint/RestoreV2	RestoreV2server_savepoint/Const,save_server_savepoint/RestoreV2/tensor_names0save_server_savepoint/RestoreV2/shape_and_slices"/device:CPU:0*
dtypes
2

T
save_server_savepoint/IdentityIdentitysave_server_savepoint/RestoreV2*
T0
И
&save_server_savepoint/AssignVariableOpAssignVariableOpmetrics/eval/losssave_server_savepoint/Identity*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
X
 save_server_savepoint/Identity_1Identity!save_server_savepoint/RestoreV2:1*
T0
а
(save_server_savepoint/AssignVariableOp_1AssignVariableOp%server/aggregator/dp_event/class_name save_server_savepoint/Identity_1*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
X
 save_server_savepoint/Identity_2Identity!save_server_savepoint/RestoreV2:2*
T0
б
(save_server_savepoint/AssignVariableOp_2AssignVariableOp&server/aggregator/dp_event/module_name save_server_savepoint/Identity_2*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
X
 save_server_savepoint/Identity_3Identity!save_server_savepoint/RestoreV2:3*
T0
ж
(save_server_savepoint/AssignVariableOp_3AssignVariableOp+server/aggregator/dp_event/noise_multiplier save_server_savepoint/Identity_3*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
X
 save_server_savepoint/Identity_4Identity!save_server_savepoint/RestoreV2:4*
T0

Ъ
(save_server_savepoint/AssignVariableOp_4AssignVariableOpserver/aggregator/is_init_state save_server_savepoint/Identity_4*
dtype0
*
validate_shape( *&
 _has_manual_control_dependencies(
X
 save_server_savepoint/Identity_5Identity!save_server_savepoint/RestoreV2:5*
T0
д
(save_server_savepoint/AssignVariableOp_5AssignVariableOp)server/aggregator/query_state/denominator save_server_savepoint/Identity_5*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
X
 save_server_savepoint/Identity_6Identity!save_server_savepoint/RestoreV2:6*
T0
х
(save_server_savepoint/AssignVariableOp_6AssignVariableOp:server/aggregator/query_state/numerator_state/l2_norm_clip save_server_savepoint/Identity_6*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
X
 save_server_savepoint/Identity_7Identity!save_server_savepoint/RestoreV2:7*
T0
п
(save_server_savepoint/AssignVariableOp_7AssignVariableOp4server/aggregator/query_state/numerator_state/stddev save_server_savepoint/Identity_7*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
X
 save_server_savepoint/Identity_8Identity!save_server_savepoint/RestoreV2:8*
T0
Щ
(save_server_savepoint/AssignVariableOp_8AssignVariableOpserver/finalizer/learning_rate save_server_savepoint/Identity_8*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
X
 save_server_savepoint/Identity_9Identity!save_server_savepoint/RestoreV2:9*
T0
в
(save_server_savepoint/AssignVariableOp_9AssignVariableOp'server/global_model_weights/trainable/0 save_server_savepoint/Identity_9*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
Z
!save_server_savepoint/Identity_10Identity"save_server_savepoint/RestoreV2:10*
T0
д
)save_server_savepoint/AssignVariableOp_10AssignVariableOp'server/global_model_weights/trainable/1!save_server_savepoint/Identity_10*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
Z
!save_server_savepoint/Identity_11Identity"save_server_savepoint/RestoreV2:11*
T0
д
)save_server_savepoint/AssignVariableOp_11AssignVariableOp'server/global_model_weights/trainable/2!save_server_savepoint/Identity_11*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
Z
!save_server_savepoint/Identity_12Identity"save_server_savepoint/RestoreV2:12*
T0
д
)save_server_savepoint/AssignVariableOp_12AssignVariableOp'server/global_model_weights/trainable/3!save_server_savepoint/Identity_12*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
й
!save_server_savepoint/restore_allNoOp'^save_server_savepoint/AssignVariableOp)^save_server_savepoint/AssignVariableOp_1*^save_server_savepoint/AssignVariableOp_10*^save_server_savepoint/AssignVariableOp_11*^save_server_savepoint/AssignVariableOp_12)^save_server_savepoint/AssignVariableOp_2)^save_server_savepoint/AssignVariableOp_3)^save_server_savepoint/AssignVariableOp_4)^save_server_savepoint/AssignVariableOp_5)^save_server_savepoint/AssignVariableOp_6)^save_server_savepoint/AssignVariableOp_7)^save_server_savepoint/AssignVariableOp_8)^save_server_savepoint/AssignVariableOp_9
M
server_savepoint/filename/inputConst*
dtype0*
valueB Bmodel
n
server_savepoint/filenamePlaceholderWithDefaultserver_savepoint/filename/input*
dtype0*
shape: 
e
server_savepoint/ConstPlaceholderWithDefaultserver_savepoint/filename*
dtype0*
shape: 
Ю
"server_savepoint/save/tensor_namesConst*
dtype0*
valueBB%server/aggregator/dp_event/class_nameB&server/aggregator/dp_event/module_nameB+server/aggregator/dp_event/noise_multiplierBserver/aggregator/is_init_stateB)server/aggregator/query_state/denominatorB:server/aggregator/query_state/numerator_state/l2_norm_clipB4server/aggregator/query_state/numerator_state/stddevBserver/finalizer/learning_rateB'server/global_model_weights/trainable/0B'server/global_model_weights/trainable/1B'server/global_model_weights/trainable/2B'server/global_model_weights/trainable/3
j
'server_savepoint/save/shapes_and_slicesConst*
dtype0*+
value"B B B B B B B B B B B B B 
Е
server_savepoint/save
SaveSlicesserver_savepoint/Const"server_savepoint/save/tensor_names'server_savepoint/save/shapes_and_slices9server/aggregator/dp_event/class_name/Read/ReadVariableOp:server/aggregator/dp_event/module_name/Read/ReadVariableOp?server/aggregator/dp_event/noise_multiplier/Read/ReadVariableOp3server/aggregator/is_init_state/Read/ReadVariableOp=server/aggregator/query_state/denominator/Read/ReadVariableOpNserver/aggregator/query_state/numerator_state/l2_norm_clip/Read/ReadVariableOpHserver/aggregator/query_state/numerator_state/stddev/Read/ReadVariableOp2server/finalizer/learning_rate/Read/ReadVariableOp;server/global_model_weights/trainable/0/Read/ReadVariableOp;server/global_model_weights/trainable/1/Read/ReadVariableOp;server/global_model_weights/trainable/2/Read/ReadVariableOp;server/global_model_weights/trainable/3/Read/ReadVariableOp*
T
2
*&
 _has_manual_control_dependencies(

#server_savepoint/control_dependencyIdentityserver_savepoint/Const^server_savepoint/save*)
_class
loc:@server_savepoint/Const*
T0
т
'server_savepoint/RestoreV2/tensor_namesConst"/device:CPU:0*
dtype0*
valueBB%server/aggregator/dp_event/class_nameB&server/aggregator/dp_event/module_nameB+server/aggregator/dp_event/noise_multiplierBserver/aggregator/is_init_stateB)server/aggregator/query_state/denominatorB:server/aggregator/query_state/numerator_state/l2_norm_clipB4server/aggregator/query_state/numerator_state/stddevBserver/finalizer/learning_rateB'server/global_model_weights/trainable/0B'server/global_model_weights/trainable/1B'server/global_model_weights/trainable/2B'server/global_model_weights/trainable/3
}
+server_savepoint/RestoreV2/shape_and_slicesConst"/device:CPU:0*
dtype0*+
value"B B B B B B B B B B B B B 
Р
server_savepoint/RestoreV2	RestoreV2server_savepoint/Const'server_savepoint/RestoreV2/tensor_names+server_savepoint/RestoreV2/shape_and_slices"/device:CPU:0*
dtypes
2

J
server_savepoint/IdentityIdentityserver_savepoint/RestoreV2*
T0
Т
!server_savepoint/AssignVariableOpAssignVariableOp%server/aggregator/dp_event/class_nameserver_savepoint/Identity*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
N
server_savepoint/Identity_1Identityserver_savepoint/RestoreV2:1*
T0
Ч
#server_savepoint/AssignVariableOp_1AssignVariableOp&server/aggregator/dp_event/module_nameserver_savepoint/Identity_1*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
N
server_savepoint/Identity_2Identityserver_savepoint/RestoreV2:2*
T0
Ь
#server_savepoint/AssignVariableOp_2AssignVariableOp+server/aggregator/dp_event/noise_multiplierserver_savepoint/Identity_2*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
N
server_savepoint/Identity_3Identityserver_savepoint/RestoreV2:3*
T0

Р
#server_savepoint/AssignVariableOp_3AssignVariableOpserver/aggregator/is_init_stateserver_savepoint/Identity_3*
dtype0
*
validate_shape( *&
 _has_manual_control_dependencies(
N
server_savepoint/Identity_4Identityserver_savepoint/RestoreV2:4*
T0
Ъ
#server_savepoint/AssignVariableOp_4AssignVariableOp)server/aggregator/query_state/denominatorserver_savepoint/Identity_4*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
N
server_savepoint/Identity_5Identityserver_savepoint/RestoreV2:5*
T0
л
#server_savepoint/AssignVariableOp_5AssignVariableOp:server/aggregator/query_state/numerator_state/l2_norm_clipserver_savepoint/Identity_5*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
N
server_savepoint/Identity_6Identityserver_savepoint/RestoreV2:6*
T0
е
#server_savepoint/AssignVariableOp_6AssignVariableOp4server/aggregator/query_state/numerator_state/stddevserver_savepoint/Identity_6*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
N
server_savepoint/Identity_7Identityserver_savepoint/RestoreV2:7*
T0
П
#server_savepoint/AssignVariableOp_7AssignVariableOpserver/finalizer/learning_rateserver_savepoint/Identity_7*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
N
server_savepoint/Identity_8Identityserver_savepoint/RestoreV2:8*
T0
Ш
#server_savepoint/AssignVariableOp_8AssignVariableOp'server/global_model_weights/trainable/0server_savepoint/Identity_8*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
N
server_savepoint/Identity_9Identityserver_savepoint/RestoreV2:9*
T0
Ш
#server_savepoint/AssignVariableOp_9AssignVariableOp'server/global_model_weights/trainable/1server_savepoint/Identity_9*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
P
server_savepoint/Identity_10Identityserver_savepoint/RestoreV2:10*
T0
Ъ
$server_savepoint/AssignVariableOp_10AssignVariableOp'server/global_model_weights/trainable/2server_savepoint/Identity_10*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
P
server_savepoint/Identity_11Identityserver_savepoint/RestoreV2:11*
T0
Ъ
$server_savepoint/AssignVariableOp_11AssignVariableOp'server/global_model_weights/trainable/3server_savepoint/Identity_11*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
ь
server_savepoint/restore_allNoOp"^server_savepoint/AssignVariableOp$^server_savepoint/AssignVariableOp_1%^server_savepoint/AssignVariableOp_10%^server_savepoint/AssignVariableOp_11$^server_savepoint/AssignVariableOp_2$^server_savepoint/AssignVariableOp_3$^server_savepoint/AssignVariableOp_4$^server_savepoint/AssignVariableOp_5$^server_savepoint/AssignVariableOp_6$^server_savepoint/AssignVariableOp_7$^server_savepoint/AssignVariableOp_8$^server_savepoint/AssignVariableOp_9
U
 client/0/0/zeros/shape_as_tensorConst*
dtype0*
valueB"     
C
client/0/0/zeros/ConstConst*
dtype0*
valueB
 *    
m
client/0/0/zerosFill client/0/0/zeros/shape_as_tensorclient/0/0/zeros/Const*

index_type0*
T0
Ї
client/0/0/0VarHandleOp*
_class
loc:@client/0/0/0*
	container *
shape:
*
shared_nameclient/0/0/0*
allowed_devices
 *
dtype0
Q
-client/0/0/0/IsInitialized/VarIsInitializedOpVarIsInitializedOpclient/0/0/0

client/0/0/0/AssignAssignVariableOpclient/0/0/0client/0/0/zeros*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
M
 client/0/0/0/Read/ReadVariableOpReadVariableOpclient/0/0/0*
dtype0
D
client/0/0/zeros_1Const*
dtype0*
valueB*    
Ђ
client/0/0/1VarHandleOp*
dtype0*
	container *
shape:*
shared_nameclient/0/0/1*
allowed_devices
 *
_class
loc:@client/0/0/1
Q
-client/0/0/1/IsInitialized/VarIsInitializedOpVarIsInitializedOpclient/0/0/1

client/0/0/1/AssignAssignVariableOpclient/0/0/1client/0/0/zeros_1*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
M
 client/0/0/1/Read/ReadVariableOpReadVariableOpclient/0/0/1*
dtype0
H
client/0/0/zeros_2Const*
dtype0*
valueB	*    
І
client/0/0/2VarHandleOp*
dtype0*
	container *
shape:	*
shared_nameclient/0/0/2*
allowed_devices
 *
_class
loc:@client/0/0/2
Q
-client/0/0/2/IsInitialized/VarIsInitializedOpVarIsInitializedOpclient/0/0/2

client/0/0/2/AssignAssignVariableOpclient/0/0/2client/0/0/zeros_2*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
M
 client/0/0/2/Read/ReadVariableOpReadVariableOpclient/0/0/2*
dtype0
C
client/0/0/zeros_3Const*
dtype0*
valueB*    
Ё
client/0/0/3VarHandleOp*
_class
loc:@client/0/0/3*
	container *
shape:*
shared_nameclient/0/0/3*
allowed_devices
 *
dtype0
Q
-client/0/0/3/IsInitialized/VarIsInitializedOpVarIsInitializedOpclient/0/0/3

client/0/0/3/AssignAssignVariableOpclient/0/0/3client/0/0/zeros_3*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
M
 client/0/0/3/Read/ReadVariableOpReadVariableOpclient/0/0/3*
dtype0
M
client_savepoint/filename/inputConst*
dtype0*
valueB Bmodel
n
client_savepoint/filenamePlaceholderWithDefaultclient_savepoint/filename/input*
dtype0*
shape: 
e
client_savepoint/ConstPlaceholderWithDefaultclient_savepoint/filename*
dtype0*
shape: 

"client_savepoint/save/tensor_namesConst*
dtype0*K
valueBB@Bclient/0/0/0Bclient/0/0/1Bclient/0/0/2Bclient/0/0/3
Z
'client_savepoint/save/shapes_and_slicesConst*
dtype0*
valueBB B B B 
Ч
client_savepoint/save
SaveSlicesclient_savepoint/Const"client_savepoint/save/tensor_names'client_savepoint/save/shapes_and_slices client/0/0/0/Read/ReadVariableOp client/0/0/1/Read/ReadVariableOp client/0/0/2/Read/ReadVariableOp client/0/0/3/Read/ReadVariableOp*
T
2*&
 _has_manual_control_dependencies(

#client_savepoint/control_dependencyIdentityclient_savepoint/Const^client_savepoint/save*)
_class
loc:@client_savepoint/Const*
T0

'client_savepoint/RestoreV2/tensor_namesConst"/device:CPU:0*
dtype0*K
valueBB@Bclient/0/0/0Bclient/0/0/1Bclient/0/0/2Bclient/0/0/3
m
+client_savepoint/RestoreV2/shape_and_slicesConst"/device:CPU:0*
dtype0*
valueBB B B B 
И
client_savepoint/RestoreV2	RestoreV2client_savepoint/Const'client_savepoint/RestoreV2/tensor_names+client_savepoint/RestoreV2/shape_and_slices"/device:CPU:0*
dtypes
2
J
client_savepoint/IdentityIdentityclient_savepoint/RestoreV2*
T0
Љ
!client_savepoint/AssignVariableOpAssignVariableOpclient/0/0/0client_savepoint/Identity*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
N
client_savepoint/Identity_1Identityclient_savepoint/RestoreV2:1*
T0
­
#client_savepoint/AssignVariableOp_1AssignVariableOpclient/0/0/1client_savepoint/Identity_1*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
N
client_savepoint/Identity_2Identityclient_savepoint/RestoreV2:2*
T0
­
#client_savepoint/AssignVariableOp_2AssignVariableOpclient/0/0/2client_savepoint/Identity_2*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
N
client_savepoint/Identity_3Identityclient_savepoint/RestoreV2:3*
T0
­
#client_savepoint/AssignVariableOp_3AssignVariableOpclient/0/0/3client_savepoint/Identity_3*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
К
client_savepoint/restore_allNoOp"^client_savepoint/AssignVariableOp$^client_savepoint/AssignVariableOp_1$^client_savepoint/AssignVariableOp_2$^client_savepoint/AssignVariableOp_3
O
"intermediate_update/0/0/loss/zerosConst*
dtype0*
valueB
 *    
г
intermediate_update/0/0/loss/0VarHandleOp*
dtype0*
	container *
shape: */
shared_name intermediate_update/0/0/loss/0*
allowed_devices
 *1
_class'
%#loc:@intermediate_update/0/0/loss/0
u
?intermediate_update/0/0/loss/0/IsInitialized/VarIsInitializedOpVarIsInitializedOpintermediate_update/0/0/loss/0
Ш
%intermediate_update/0/0/loss/0/AssignAssignVariableOpintermediate_update/0/0/loss/0"intermediate_update/0/0/loss/zeros*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
q
2intermediate_update/0/0/loss/0/Read/ReadVariableOpReadVariableOpintermediate_update/0/0/loss/0*
dtype0
Q
$intermediate_update/0/0/loss/zeros_1Const*
dtype0*
valueB
 *    
г
intermediate_update/0/0/loss/1VarHandleOp*
dtype0*
	container *
shape: */
shared_name intermediate_update/0/0/loss/1*
allowed_devices
 *1
_class'
%#loc:@intermediate_update/0/0/loss/1
u
?intermediate_update/0/0/loss/1/IsInitialized/VarIsInitializedOpVarIsInitializedOpintermediate_update/0/0/loss/1
Ъ
%intermediate_update/0/0/loss/1/AssignAssignVariableOpintermediate_update/0/0/loss/1$intermediate_update/0/0/loss/zeros_1*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
q
2intermediate_update/0/0/loss/1/Read/ReadVariableOpReadVariableOpintermediate_update/0/0/loss/1*
dtype0
Z
,intermediate_update_savepoint/filename/inputConst*
dtype0*
valueB Bmodel

&intermediate_update_savepoint/filenamePlaceholderWithDefault,intermediate_update_savepoint/filename/input*
dtype0*
shape: 

#intermediate_update_savepoint/ConstPlaceholderWithDefault&intermediate_update_savepoint/filename*
dtype0*
shape: 

/intermediate_update_savepoint/save/tensor_namesConst*
dtype0*S
valueJBHBintermediate_update/0/0/loss/0Bintermediate_update/0/0/loss/1
c
4intermediate_update_savepoint/save/shapes_and_slicesConst*
dtype0*
valueBB B 
й
"intermediate_update_savepoint/save
SaveSlices#intermediate_update_savepoint/Const/intermediate_update_savepoint/save/tensor_names4intermediate_update_savepoint/save/shapes_and_slices2intermediate_update/0/0/loss/0/Read/ReadVariableOp2intermediate_update/0/0/loss/1/Read/ReadVariableOp*
T
2*&
 _has_manual_control_dependencies(
Ч
0intermediate_update_savepoint/control_dependencyIdentity#intermediate_update_savepoint/Const#^intermediate_update_savepoint/save*6
_class,
*(loc:@intermediate_update_savepoint/Const*
T0
Ў
4intermediate_update_savepoint/RestoreV2/tensor_namesConst"/device:CPU:0*
dtype0*S
valueJBHBintermediate_update/0/0/loss/0Bintermediate_update/0/0/loss/1
v
8intermediate_update_savepoint/RestoreV2/shape_and_slicesConst"/device:CPU:0*
dtype0*
valueBB B 
ъ
'intermediate_update_savepoint/RestoreV2	RestoreV2#intermediate_update_savepoint/Const4intermediate_update_savepoint/RestoreV2/tensor_names8intermediate_update_savepoint/RestoreV2/shape_and_slices"/device:CPU:0*
dtypes
2
d
&intermediate_update_savepoint/IdentityIdentity'intermediate_update_savepoint/RestoreV2*
T0
е
.intermediate_update_savepoint/AssignVariableOpAssignVariableOpintermediate_update/0/0/loss/0&intermediate_update_savepoint/Identity*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
h
(intermediate_update_savepoint/Identity_1Identity)intermediate_update_savepoint/RestoreV2:1*
T0
й
0intermediate_update_savepoint/AssignVariableOp_1AssignVariableOpintermediate_update/0/0/loss/1(intermediate_update_savepoint/Identity_1*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(

)intermediate_update_savepoint/restore_allNoOp/^intermediate_update_savepoint/AssignVariableOp1^intermediate_update_savepoint/AssignVariableOp_1


group_depsNoOp

group_deps_1NoOp
M
 aggregated_update/0/0/loss/zerosConst*
dtype0*
valueB
 *    
Э
aggregated_update/0/0/loss/0VarHandleOp*/
_class%
#!loc:@aggregated_update/0/0/loss/0*
	container *
shape: *-
shared_nameaggregated_update/0/0/loss/0*
allowed_devices
 *
dtype0
q
=aggregated_update/0/0/loss/0/IsInitialized/VarIsInitializedOpVarIsInitializedOpaggregated_update/0/0/loss/0
Т
#aggregated_update/0/0/loss/0/AssignAssignVariableOpaggregated_update/0/0/loss/0 aggregated_update/0/0/loss/zeros*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
m
0aggregated_update/0/0/loss/0/Read/ReadVariableOpReadVariableOpaggregated_update/0/0/loss/0*
dtype0
O
"aggregated_update/0/0/loss/zeros_1Const*
dtype0*
valueB
 *    
Э
aggregated_update/0/0/loss/1VarHandleOp*
dtype0*
	container *
shape: *-
shared_nameaggregated_update/0/0/loss/1*
allowed_devices
 */
_class%
#!loc:@aggregated_update/0/0/loss/1
q
=aggregated_update/0/0/loss/1/IsInitialized/VarIsInitializedOpVarIsInitializedOpaggregated_update/0/0/loss/1
Ф
#aggregated_update/0/0/loss/1/AssignAssignVariableOpaggregated_update/0/0/loss/1"aggregated_update/0/0/loss/zeros_1*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
m
0aggregated_update/0/0/loss/1/Read/ReadVariableOpReadVariableOpaggregated_update/0/0/loss/1*
dtype0
X
*aggregated_update_savepoint/filename/inputConst*
dtype0*
valueB Bmodel

$aggregated_update_savepoint/filenamePlaceholderWithDefault*aggregated_update_savepoint/filename/input*
dtype0*
shape: 
{
!aggregated_update_savepoint/ConstPlaceholderWithDefault$aggregated_update_savepoint/filename*
dtype0*
shape: 

-aggregated_update_savepoint/save/tensor_namesConst*
dtype0*O
valueFBDBaggregated_update/0/0/loss/0Baggregated_update/0/0/loss/1
a
2aggregated_update_savepoint/save/shapes_and_slicesConst*
dtype0*
valueBB B 
Э
 aggregated_update_savepoint/save
SaveSlices!aggregated_update_savepoint/Const-aggregated_update_savepoint/save/tensor_names2aggregated_update_savepoint/save/shapes_and_slices0aggregated_update/0/0/loss/0/Read/ReadVariableOp0aggregated_update/0/0/loss/1/Read/ReadVariableOp*
T
2*&
 _has_manual_control_dependencies(
П
.aggregated_update_savepoint/control_dependencyIdentity!aggregated_update_savepoint/Const!^aggregated_update_savepoint/save*4
_class*
(&loc:@aggregated_update_savepoint/Const*
T0
Ј
2aggregated_update_savepoint/RestoreV2/tensor_namesConst"/device:CPU:0*
dtype0*O
valueFBDBaggregated_update/0/0/loss/0Baggregated_update/0/0/loss/1
t
6aggregated_update_savepoint/RestoreV2/shape_and_slicesConst"/device:CPU:0*
dtype0*
valueBB B 
т
%aggregated_update_savepoint/RestoreV2	RestoreV2!aggregated_update_savepoint/Const2aggregated_update_savepoint/RestoreV2/tensor_names6aggregated_update_savepoint/RestoreV2/shape_and_slices"/device:CPU:0*
dtypes
2
`
$aggregated_update_savepoint/IdentityIdentity%aggregated_update_savepoint/RestoreV2*
T0
Я
,aggregated_update_savepoint/AssignVariableOpAssignVariableOpaggregated_update/0/0/loss/0$aggregated_update_savepoint/Identity*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
d
&aggregated_update_savepoint/Identity_1Identity'aggregated_update_savepoint/RestoreV2:1*
T0
г
.aggregated_update_savepoint/AssignVariableOp_1AssignVariableOpaggregated_update/0/0/loss/1&aggregated_update_savepoint/Identity_1*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(

'aggregated_update_savepoint/restore_allNoOp-^aggregated_update_savepoint/AssignVariableOp/^aggregated_update_savepoint/AssignVariableOp_1
Ћ
*initialize_server_state_and_non_state_varsNoOp^metrics/eval/loss/Assign-^server/aggregator/dp_event/class_name/Assign.^server/aggregator/dp_event/module_name/Assign3^server/aggregator/dp_event/noise_multiplier/Assign'^server/aggregator/is_init_state/Assign1^server/aggregator/query_state/denominator/AssignB^server/aggregator/query_state/numerator_state/l2_norm_clip/Assign<^server/aggregator/query_state/numerator_state/stddev/Assign&^server/finalizer/learning_rate/Assign/^server/global_model_weights/trainable/0/Assign/^server/global_model_weights/trainable/1/Assign/^server/global_model_weights/trainable/2/Assign/^server/global_model_weights/trainable/3/Assign
@
update/0/loss/zerosConst*
dtype0*
valueB
 *    
І
update/0/loss/0VarHandleOp*
dtype0*
	container *
shape: * 
shared_nameupdate/0/loss/0*
allowed_devices
 *"
_class
loc:@update/0/loss/0
W
0update/0/loss/0/IsInitialized/VarIsInitializedOpVarIsInitializedOpupdate/0/loss/0

update/0/loss/0/AssignAssignVariableOpupdate/0/loss/0update/0/loss/zeros*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
S
#update/0/loss/0/Read/ReadVariableOpReadVariableOpupdate/0/loss/0*
dtype0
B
update/0/loss/zeros_1Const*
dtype0*
valueB
 *    
І
update/0/loss/1VarHandleOp*"
_class
loc:@update/0/loss/1*
	container *
shape: * 
shared_nameupdate/0/loss/1*
allowed_devices
 *
dtype0
W
0update/0/loss/1/IsInitialized/VarIsInitializedOpVarIsInitializedOpupdate/0/loss/1

update/0/loss/1/AssignAssignVariableOpupdate/0/loss/1update/0/loss/zeros_1*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
S
#update/0/loss/1/Read/ReadVariableOpReadVariableOpupdate/0/loss/1*
dtype0
M
update_savepoint/filename/inputConst*
dtype0*
valueB Bmodel
n
update_savepoint/filenamePlaceholderWithDefaultupdate_savepoint/filename/input*
dtype0*
shape: 
e
update_savepoint/ConstPlaceholderWithDefaultupdate_savepoint/filename*
dtype0*
shape: 
o
"update_savepoint/save/tensor_namesConst*
dtype0*5
value,B*Bupdate/0/loss/0Bupdate/0/loss/1
V
'update_savepoint/save/shapes_and_slicesConst*
dtype0*
valueBB B 

update_savepoint/save
SaveSlicesupdate_savepoint/Const"update_savepoint/save/tensor_names'update_savepoint/save/shapes_and_slices#update/0/loss/0/Read/ReadVariableOp#update/0/loss/1/Read/ReadVariableOp*
T
2*&
 _has_manual_control_dependencies(

#update_savepoint/control_dependencyIdentityupdate_savepoint/Const^update_savepoint/save*)
_class
loc:@update_savepoint/Const*
T0

'update_savepoint/RestoreV2/tensor_namesConst"/device:CPU:0*
dtype0*5
value,B*Bupdate/0/loss/0Bupdate/0/loss/1
i
+update_savepoint/RestoreV2/shape_and_slicesConst"/device:CPU:0*
dtype0*
valueBB B 
Ж
update_savepoint/RestoreV2	RestoreV2update_savepoint/Const'update_savepoint/RestoreV2/tensor_names+update_savepoint/RestoreV2/shape_and_slices"/device:CPU:0*
dtypes
2
J
update_savepoint/IdentityIdentityupdate_savepoint/RestoreV2*
T0
Ќ
!update_savepoint/AssignVariableOpAssignVariableOpupdate/0/loss/0update_savepoint/Identity*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
N
update_savepoint/Identity_1Identityupdate_savepoint/RestoreV2:1*
T0
А
#update_savepoint/AssignVariableOp_1AssignVariableOpupdate/0/loss/1update_savepoint/Identity_1*
dtype0*
validate_shape( *&
 _has_manual_control_dependencies(
n
update_savepoint/restore_allNoOp"^update_savepoint/AssignVariableOp$^update_savepoint/AssignVariableOp_1
P
initialize_update_varsNoOp^update/0/loss/0/Assign^update/0/loss/1/Assign
ч
initialize_accumulator_varsNoOp$^aggregated_update/0/0/loss/0/Assign$^aggregated_update/0/0/loss/1/Assign&^intermediate_update/0/0/loss/0/Assign&^intermediate_update/0/0/loss/1/Assign*&
 _has_manual_control_dependencies(
Z
zero/Identity_1Const^initialize_accumulator_vars*
dtype0*
valueB
 "    
`
zero/session_token_tensorPlaceholder^initialize_accumulator_vars*
dtype0*
shape: 
X
zero/IdentityConst^initialize_accumulator_vars*
dtype0*
valueB
 "    

AssignVariableOpAssignVariableOpintermediate_update/0/0/loss/0zero/Identity*
dtype0*
validate_shape(*&
 _has_manual_control_dependencies(
~
ReadVariableOpReadVariableOpintermediate_update/0/0/loss/0^AssignVariableOp^initialize_accumulator_vars*
dtype0
Ђ
AssignVariableOp_1AssignVariableOpintermediate_update/0/0/loss/1zero/Identity_1*
dtype0*
validate_shape(*&
 _has_manual_control_dependencies(

ReadVariableOp_1ReadVariableOpintermediate_update/0/0/loss/1^AssignVariableOp_1^initialize_accumulator_vars*
dtype0

AssignVariableOp_2AssignVariableOpaggregated_update/0/0/loss/0zero/Identity*
dtype0*
validate_shape(*&
 _has_manual_control_dependencies(

ReadVariableOp_2ReadVariableOpaggregated_update/0/0/loss/0^AssignVariableOp_2^initialize_accumulator_vars*
dtype0
 
AssignVariableOp_3AssignVariableOpaggregated_update/0/0/loss/1zero/Identity_1*
dtype0*
validate_shape(*&
 _has_manual_control_dependencies(

ReadVariableOp_3ReadVariableOpaggregated_update/0/0/loss/1^AssignVariableOp_3^initialize_accumulator_vars*
dtype0
Ј
 initialize_client_vars_on_serverNoOp^client/0/0/0/Assign^client/0/0/1/Assign^client/0/0/2/Assign^client/0/0/3/Assign*&
 _has_manual_control_dependencies(
l
 write_client_session_token/inputConst!^initialize_client_vars_on_server*
dtype0*
valueB B 
p
write_client_session_tokenPlaceholderWithDefault write_client_session_token/input*
dtype0*
shape: 
~
Read/ReadVariableOpReadVariableOp'server/global_model_weights/trainable/0!^initialize_client_vars_on_server*
dtype0
2
IdentityIdentityRead/ReadVariableOp*
T0

Read_1/ReadVariableOpReadVariableOp'server/global_model_weights/trainable/1!^initialize_client_vars_on_server*
dtype0
6

Identity_1IdentityRead_1/ReadVariableOp*
T0

Read_2/ReadVariableOpReadVariableOp'server/global_model_weights/trainable/2!^initialize_client_vars_on_server*
dtype0
6

Identity_2IdentityRead_2/ReadVariableOp*
T0

Read_3/ReadVariableOpReadVariableOp'server/global_model_weights/trainable/3!^initialize_client_vars_on_server*
dtype0
6

Identity_3IdentityRead_3/ReadVariableOp*
T0

Read_4/ReadVariableOpReadVariableOp:server/aggregator/query_state/numerator_state/l2_norm_clip!^initialize_client_vars_on_server*
dtype0
6

Identity_4IdentityRead_4/ReadVariableOp*
T0

Read_5/ReadVariableOpReadVariableOp4server/aggregator/query_state/numerator_state/stddev!^initialize_client_vars_on_server*
dtype0
6

Identity_5IdentityRead_5/ReadVariableOp*
T0

Read_6/ReadVariableOpReadVariableOp)server/aggregator/query_state/denominator!^initialize_client_vars_on_server*
dtype0
6

Identity_6IdentityRead_6/ReadVariableOp*
T0

Read_7/ReadVariableOpReadVariableOp&server/aggregator/dp_event/module_name!^initialize_client_vars_on_server*
dtype0
6

Identity_7IdentityRead_7/ReadVariableOp*
T0
~
Read_8/ReadVariableOpReadVariableOp%server/aggregator/dp_event/class_name!^initialize_client_vars_on_server*
dtype0
6

Identity_8IdentityRead_8/ReadVariableOp*
T0

Read_9/ReadVariableOpReadVariableOp+server/aggregator/dp_event/noise_multiplier!^initialize_client_vars_on_server*
dtype0
6

Identity_9IdentityRead_9/ReadVariableOp*
T0
y
Read_10/ReadVariableOpReadVariableOpserver/aggregator/is_init_state!^initialize_client_vars_on_server*
dtype0

8
Identity_10IdentityRead_10/ReadVariableOp*
T0

x
Read_11/ReadVariableOpReadVariableOpserver/finalizer/learning_rate!^initialize_client_vars_on_server*
dtype0
8
Identity_11IdentityRead_11/ReadVariableOp*
T0
j
prepare/_inputs/ConstConst!^initialize_client_vars_on_server*
dtype0*
valueB B	^Identity
n
prepare/_inputs/Const_1Const!^initialize_client_vars_on_server*
dtype0*
valueB B^Identity_1
n
prepare/_inputs/Const_2Const!^initialize_client_vars_on_server*
dtype0*
valueB B^Identity_2
n
prepare/_inputs/Const_3Const!^initialize_client_vars_on_server*
dtype0*
valueB B^Identity_3
n
prepare/_inputs/Const_4Const!^initialize_client_vars_on_server*
dtype0*
valueB B^Identity_4
n
prepare/_inputs/Const_5Const!^initialize_client_vars_on_server*
dtype0*
valueB B^Identity_5
n
prepare/_inputs/Const_6Const!^initialize_client_vars_on_server*
dtype0*
valueB B^Identity_6
n
prepare/_inputs/Const_7Const!^initialize_client_vars_on_server*
dtype0*
valueB B^Identity_7
n
prepare/_inputs/Const_8Const!^initialize_client_vars_on_server*
dtype0*
valueB B^Identity_8
n
prepare/_inputs/Const_9Const!^initialize_client_vars_on_server*
dtype0*
valueB B^Identity_9
p
prepare/_inputs/Const_10Const!^initialize_client_vars_on_server*
dtype0*
valueB B^Identity_10
p
prepare/_inputs/Const_11Const!^initialize_client_vars_on_server*
dtype0*
valueB B^Identity_11
v
*prepare/arg_aggregator_dp_event_class_namePlaceholder!^initialize_client_vars_on_server*
dtype0*
shape: 
w
+prepare/arg_aggregator_dp_event_module_namePlaceholder!^initialize_client_vars_on_server*
dtype0*
shape: 
|
0prepare/arg_aggregator_dp_event_noise_multiplierPlaceholder!^initialize_client_vars_on_server*
dtype0*
shape: 
p
$prepare/arg_aggregator_is_init_statePlaceholder!^initialize_client_vars_on_server*
dtype0
*
shape: 
z
.prepare/arg_aggregator_query_state_denominatorPlaceholder!^initialize_client_vars_on_server*
dtype0*
shape: 

?prepare/arg_aggregator_query_state_numerator_state_l2_norm_clipPlaceholder!^initialize_client_vars_on_server*
dtype0*
shape: 

9prepare/arg_aggregator_query_state_numerator_state_stddevPlaceholder!^initialize_client_vars_on_server*
dtype0*
shape: 
o
#prepare/arg_finalizer_learning_ratePlaceholder!^initialize_client_vars_on_server*
dtype0*
shape: 

/prepare/arg_global_model_weights_trainable_NonePlaceholder!^initialize_client_vars_on_server*
dtype0*
shape:


1prepare/arg_global_model_weights_trainable_None_1Placeholder!^initialize_client_vars_on_server*
dtype0*
shape:

1prepare/arg_global_model_weights_trainable_None_2Placeholder!^initialize_client_vars_on_server*
dtype0*
shape:	

1prepare/arg_global_model_weights_trainable_None_3Placeholder!^initialize_client_vars_on_server*
dtype0*
shape:
h
prepare/session_token_tensorPlaceholder!^initialize_client_vars_on_server*
dtype0*
shape: 

9prepare/subcomputation/arg_aggregator_dp_event_class_namePlaceholder!^initialize_client_vars_on_server*
dtype0*
shape: 

:prepare/subcomputation/arg_aggregator_dp_event_module_namePlaceholder!^initialize_client_vars_on_server*
dtype0*
shape: 

?prepare/subcomputation/arg_aggregator_dp_event_noise_multiplierPlaceholder!^initialize_client_vars_on_server*
dtype0*
shape: 

3prepare/subcomputation/arg_aggregator_is_init_statePlaceholder!^initialize_client_vars_on_server*
dtype0
*
shape: 

=prepare/subcomputation/arg_aggregator_query_state_denominatorPlaceholder!^initialize_client_vars_on_server*
dtype0*
shape: 

Nprepare/subcomputation/arg_aggregator_query_state_numerator_state_l2_norm_clipPlaceholder!^initialize_client_vars_on_server*
dtype0*
shape: 

Hprepare/subcomputation/arg_aggregator_query_state_numerator_state_stddevPlaceholder!^initialize_client_vars_on_server*
dtype0*
shape: 
~
2prepare/subcomputation/arg_finalizer_learning_ratePlaceholder!^initialize_client_vars_on_server*
dtype0*
shape: 

>prepare/subcomputation/arg_global_model_weights_trainable_NonePlaceholder!^initialize_client_vars_on_server*
dtype0*
shape:


@prepare/subcomputation/arg_global_model_weights_trainable_None_1Placeholder!^initialize_client_vars_on_server*
dtype0*
shape:

@prepare/subcomputation/arg_global_model_weights_trainable_None_2Placeholder!^initialize_client_vars_on_server*
dtype0*
shape:	

@prepare/subcomputation/arg_global_model_weights_trainable_None_3Placeholder!^initialize_client_vars_on_server*
dtype0*
shape:
w
+prepare/subcomputation/session_token_tensorPlaceholder!^initialize_client_vars_on_server*
dtype0*
shape: 

Hprepare/subcomputation/subcomputation/arg_aggregator_dp_event_class_namePlaceholder!^initialize_client_vars_on_server*
dtype0*
shape: 

Iprepare/subcomputation/subcomputation/arg_aggregator_dp_event_module_namePlaceholder!^initialize_client_vars_on_server*
dtype0*
shape: 

Nprepare/subcomputation/subcomputation/arg_aggregator_dp_event_noise_multiplierPlaceholder!^initialize_client_vars_on_server*
dtype0*
shape: 

Bprepare/subcomputation/subcomputation/arg_aggregator_is_init_statePlaceholder!^initialize_client_vars_on_server*
dtype0
*
shape: 

Lprepare/subcomputation/subcomputation/arg_aggregator_query_state_denominatorPlaceholder!^initialize_client_vars_on_server*
dtype0*
shape: 
Љ
]prepare/subcomputation/subcomputation/arg_aggregator_query_state_numerator_state_l2_norm_clipPlaceholder!^initialize_client_vars_on_server*
dtype0*
shape: 
Ѓ
Wprepare/subcomputation/subcomputation/arg_aggregator_query_state_numerator_state_stddevPlaceholder!^initialize_client_vars_on_server*
dtype0*
shape: 

Aprepare/subcomputation/subcomputation/arg_finalizer_learning_ratePlaceholder!^initialize_client_vars_on_server*
dtype0*
shape: 
Ѓ
Mprepare/subcomputation/subcomputation/arg_global_model_weights_trainable_NonePlaceholder!^initialize_client_vars_on_server*
dtype0*
shape:

 
Oprepare/subcomputation/subcomputation/arg_global_model_weights_trainable_None_1Placeholder!^initialize_client_vars_on_server*
dtype0*
shape:
Є
Oprepare/subcomputation/subcomputation/arg_global_model_weights_trainable_None_2Placeholder!^initialize_client_vars_on_server*
dtype0*
shape:	

Oprepare/subcomputation/subcomputation/arg_global_model_weights_trainable_None_3Placeholder!^initialize_client_vars_on_server*
dtype0*
shape:

:prepare/subcomputation/subcomputation/session_token_tensorPlaceholder!^initialize_client_vars_on_server*
dtype0*
shape: 
U
prepare/Identity_12IdentityIdentity!^initialize_client_vars_on_server*
T0
W
prepare/Identity_13Identity
Identity_1!^initialize_client_vars_on_server*
T0
W
prepare/Identity_14Identity
Identity_2!^initialize_client_vars_on_server*
T0
W
prepare/Identity_15Identity
Identity_3!^initialize_client_vars_on_server*
T0

AssignVariableOp_4AssignVariableOpclient/0/0/0prepare/Identity_12*
dtype0*
validate_shape(*&
 _has_manual_control_dependencies(
u
ReadVariableOp_4ReadVariableOpclient/0/0/0^AssignVariableOp_4!^initialize_client_vars_on_server*
dtype0

AssignVariableOp_5AssignVariableOpclient/0/0/1prepare/Identity_13*
dtype0*
validate_shape(*&
 _has_manual_control_dependencies(
u
ReadVariableOp_5ReadVariableOpclient/0/0/1^AssignVariableOp_5!^initialize_client_vars_on_server*
dtype0

AssignVariableOp_6AssignVariableOpclient/0/0/2prepare/Identity_14*
dtype0*
validate_shape(*&
 _has_manual_control_dependencies(
u
ReadVariableOp_6ReadVariableOpclient/0/0/2^AssignVariableOp_6!^initialize_client_vars_on_server*
dtype0

AssignVariableOp_7AssignVariableOpclient/0/0/3prepare/Identity_15*
dtype0*
validate_shape(*&
 _has_manual_control_dependencies(
u
ReadVariableOp_7ReadVariableOpclient/0/0/3^AssignVariableOp_7!^initialize_client_vars_on_server*
dtype0
h
group_deps_2NoOp^AssignVariableOp_4^AssignVariableOp_5^AssignVariableOp_6^AssignVariableOp_7
U
Read_12/ReadVariableOpReadVariableOpintermediate_update/0/0/loss/0*
dtype0
8
Identity_12IdentityRead_12/ReadVariableOp*
T0
U
Read_13/ReadVariableOpReadVariableOpintermediate_update/0/0/loss/1*
dtype0
8
Identity_13IdentityRead_13/ReadVariableOp*
T0
F
Read_14/ReadVariableOpReadVariableOpupdate/0/loss/0*
dtype0
8
Identity_14IdentityRead_14/ReadVariableOp*
T0
F
Read_15/ReadVariableOpReadVariableOpupdate/0/loss/1*
dtype0
8
Identity_15IdentityRead_15/ReadVariableOp*
T0
M
accumulate/_inputs/ConstConst*
dtype0*
valueB B^Identity_12
O
accumulate/_inputs/Const_1Const*
dtype0*
valueB B^Identity_13
O
accumulate/_inputs/Const_2Const*
dtype0*
valueB B^Identity_14
O
accumulate/_inputs/Const_3Const*
dtype0*
valueB B^Identity_15
K
"accumulate/arg_None_None_loss_NonePlaceholder*
dtype0*
shape: 
M
$accumulate/arg_None_None_loss_None_1Placeholder*
dtype0*
shape: 
M
$accumulate/arg_None_None_loss_None_2Placeholder*
dtype0*
shape: 
M
$accumulate/arg_None_None_loss_None_3Placeholder*
dtype0*
shape: 
H
accumulate/session_token_tensorPlaceholder*
dtype0*
shape: 
N
%accumulate/subcomputation/x_loss_NonePlaceholder*
dtype0*
shape: 
P
'accumulate/subcomputation/x_loss_None_1Placeholder*
dtype0*
shape: 
N
%accumulate/subcomputation/y_loss_NonePlaceholder*
dtype0*
shape: 
P
'accumulate/subcomputation/y_loss_None_1Placeholder*
dtype0*
shape: 
I
accumulate/subcomputation/AddAddV2Identity_12Identity_14*
T0
K
accumulate/subcomputation/Add_1AddV2Identity_13Identity_15*
T0
I
accumulate/Identity_4Identityaccumulate/subcomputation/Add*
T0
K
accumulate/Identity_5Identityaccumulate/subcomputation/Add_1*
T0
Ј
AssignVariableOp_8AssignVariableOpintermediate_update/0/0/loss/0accumulate/Identity_4*
dtype0*
validate_shape(*&
 _has_manual_control_dependencies(
d
ReadVariableOp_8ReadVariableOpintermediate_update/0/0/loss/0^AssignVariableOp_8*
dtype0
Ј
AssignVariableOp_9AssignVariableOpintermediate_update/0/0/loss/1accumulate/Identity_5*
dtype0*
validate_shape(*&
 _has_manual_control_dependencies(
d
ReadVariableOp_9ReadVariableOpintermediate_update/0/0/loss/1^AssignVariableOp_9*
dtype0
>
group_deps_3NoOp^AssignVariableOp_8^AssignVariableOp_9
4
initNoOp*&
 _has_manual_control_dependencies(
m
group_deps_4NoOp^AssignVariableOp^AssignVariableOp_1^AssignVariableOp_2^AssignVariableOp_3^init
S
Read_16/ReadVariableOpReadVariableOpaggregated_update/0/0/loss/0*
dtype0
8
Identity_16IdentityRead_16/ReadVariableOp*
T0
S
Read_17/ReadVariableOpReadVariableOpaggregated_update/0/0/loss/1*
dtype0
8
Identity_17IdentityRead_17/ReadVariableOp*
T0
U
Read_18/ReadVariableOpReadVariableOpintermediate_update/0/0/loss/0*
dtype0
8
Identity_18IdentityRead_18/ReadVariableOp*
T0
U
Read_19/ReadVariableOpReadVariableOpintermediate_update/0/0/loss/1*
dtype0
8
Identity_19IdentityRead_19/ReadVariableOp*
T0
H
merge/_inputs/ConstConst*
dtype0*
valueB B^Identity_16
J
merge/_inputs/Const_1Const*
dtype0*
valueB B^Identity_17
J
merge/_inputs/Const_2Const*
dtype0*
valueB B^Identity_18
J
merge/_inputs/Const_3Const*
dtype0*
valueB B^Identity_19
F
merge/arg_None_None_loss_NonePlaceholder*
dtype0*
shape: 
H
merge/arg_None_None_loss_None_1Placeholder*
dtype0*
shape: 
H
merge/arg_None_None_loss_None_2Placeholder*
dtype0*
shape: 
H
merge/arg_None_None_loss_None_3Placeholder*
dtype0*
shape: 
C
merge/session_token_tensorPlaceholder*
dtype0*
shape: 
I
 merge/subcomputation/x_loss_NonePlaceholder*
dtype0*
shape: 
K
"merge/subcomputation/x_loss_None_1Placeholder*
dtype0*
shape: 
I
 merge/subcomputation/y_loss_NonePlaceholder*
dtype0*
shape: 
K
"merge/subcomputation/y_loss_None_1Placeholder*
dtype0*
shape: 
D
merge/subcomputation/AddAddV2Identity_16Identity_18*
T0
F
merge/subcomputation/Add_1AddV2Identity_17Identity_19*
T0
?
merge/Identity_4Identitymerge/subcomputation/Add*
T0
A
merge/Identity_5Identitymerge/subcomputation/Add_1*
T0
Ђ
AssignVariableOp_10AssignVariableOpaggregated_update/0/0/loss/0merge/Identity_4*
dtype0*
validate_shape(*&
 _has_manual_control_dependencies(
d
ReadVariableOp_10ReadVariableOpaggregated_update/0/0/loss/0^AssignVariableOp_10*
dtype0
Ђ
AssignVariableOp_11AssignVariableOpaggregated_update/0/0/loss/1merge/Identity_5*
dtype0*
validate_shape(*&
 _has_manual_control_dependencies(
d
ReadVariableOp_11ReadVariableOpaggregated_update/0/0/loss/1^AssignVariableOp_11*
dtype0
@
group_deps_5NoOp^AssignVariableOp_10^AssignVariableOp_11
S
Read_20/ReadVariableOpReadVariableOpaggregated_update/0/0/loss/0*
dtype0
8
Identity_20IdentityRead_20/ReadVariableOp*
T0
S
Read_21/ReadVariableOpReadVariableOpaggregated_update/0/0/loss/1*
dtype0
8
Identity_21IdentityRead_21/ReadVariableOp*
T0
I
report/_inputs/ConstConst*
dtype0*
valueB B^Identity_20
K
report/_inputs/Const_1Const*
dtype0*
valueB B^Identity_21
B
report/arg_None_loss_NonePlaceholder*
dtype0*
shape: 
D
report/arg_None_loss_None_1Placeholder*
dtype0*
shape: 
D
report/session_token_tensorPlaceholder*
dtype0*
shape: 
1
report/IdentityIdentityIdentity_20*
T0
3
report/Identity_1IdentityIdentity_21*
T0
^
Read_22/ReadVariableOpReadVariableOp'server/global_model_weights/trainable/0*
dtype0
8
Identity_22IdentityRead_22/ReadVariableOp*
T0
^
Read_23/ReadVariableOpReadVariableOp'server/global_model_weights/trainable/1*
dtype0
8
Identity_23IdentityRead_23/ReadVariableOp*
T0
^
Read_24/ReadVariableOpReadVariableOp'server/global_model_weights/trainable/2*
dtype0
8
Identity_24IdentityRead_24/ReadVariableOp*
T0
^
Read_25/ReadVariableOpReadVariableOp'server/global_model_weights/trainable/3*
dtype0
8
Identity_25IdentityRead_25/ReadVariableOp*
T0
q
Read_26/ReadVariableOpReadVariableOp:server/aggregator/query_state/numerator_state/l2_norm_clip*
dtype0
8
Identity_26IdentityRead_26/ReadVariableOp*
T0
k
Read_27/ReadVariableOpReadVariableOp4server/aggregator/query_state/numerator_state/stddev*
dtype0
8
Identity_27IdentityRead_27/ReadVariableOp*
T0
`
Read_28/ReadVariableOpReadVariableOp)server/aggregator/query_state/denominator*
dtype0
8
Identity_28IdentityRead_28/ReadVariableOp*
T0
]
Read_29/ReadVariableOpReadVariableOp&server/aggregator/dp_event/module_name*
dtype0
8
Identity_29IdentityRead_29/ReadVariableOp*
T0
\
Read_30/ReadVariableOpReadVariableOp%server/aggregator/dp_event/class_name*
dtype0
8
Identity_30IdentityRead_30/ReadVariableOp*
T0
b
Read_31/ReadVariableOpReadVariableOp+server/aggregator/dp_event/noise_multiplier*
dtype0
8
Identity_31IdentityRead_31/ReadVariableOp*
T0
V
Read_32/ReadVariableOpReadVariableOpserver/aggregator/is_init_state*
dtype0

8
Identity_32IdentityRead_32/ReadVariableOp*
T0

U
Read_33/ReadVariableOpReadVariableOpserver/finalizer/learning_rate*
dtype0
8
Identity_33IdentityRead_33/ReadVariableOp*
T0
K
update_1/_inputs/ConstConst*
dtype0*
valueB B^Identity_22
M
update_1/_inputs/Const_1Const*
dtype0*
valueB B^Identity_23
M
update_1/_inputs/Const_2Const*
dtype0*
valueB B^Identity_24
M
update_1/_inputs/Const_3Const*
dtype0*
valueB B^Identity_25
M
update_1/_inputs/Const_4Const*
dtype0*
valueB B^Identity_26
M
update_1/_inputs/Const_5Const*
dtype0*
valueB B^Identity_27
M
update_1/_inputs/Const_6Const*
dtype0*
valueB B^Identity_28
M
update_1/_inputs/Const_7Const*
dtype0*
valueB B^Identity_29
M
update_1/_inputs/Const_8Const*
dtype0*
valueB B^Identity_30
M
update_1/_inputs/Const_9Const*
dtype0*
valueB B^Identity_31
N
update_1/_inputs/Const_10Const*
dtype0*
valueB B^Identity_32
N
update_1/_inputs/Const_11Const*
dtype0*
valueB B^Identity_33
R
update_1/_inputs/Const_12Const*
dtype0*!
valueB B^report/Identity
T
update_1/_inputs/Const_13Const*
dtype0*#
valueB B^report/Identity_1
N
%update_1/arg_None_None_None_loss_NonePlaceholder*
dtype0*
shape: 
P
'update_1/arg_None_None_None_loss_None_1Placeholder*
dtype0*
shape: 
Y
0update_1/arg_None_aggregator_dp_event_class_namePlaceholder*
dtype0*
shape: 
Z
1update_1/arg_None_aggregator_dp_event_module_namePlaceholder*
dtype0*
shape: 
_
6update_1/arg_None_aggregator_dp_event_noise_multiplierPlaceholder*
dtype0*
shape: 
S
*update_1/arg_None_aggregator_is_init_statePlaceholder*
dtype0
*
shape: 
]
4update_1/arg_None_aggregator_query_state_denominatorPlaceholder*
dtype0*
shape: 
n
Eupdate_1/arg_None_aggregator_query_state_numerator_state_l2_norm_clipPlaceholder*
dtype0*
shape: 
h
?update_1/arg_None_aggregator_query_state_numerator_state_stddevPlaceholder*
dtype0*
shape: 
R
)update_1/arg_None_finalizer_learning_ratePlaceholder*
dtype0*
shape: 
h
5update_1/arg_None_global_model_weights_trainable_NonePlaceholder*
dtype0*
shape:

e
7update_1/arg_None_global_model_weights_trainable_None_1Placeholder*
dtype0*
shape:
i
7update_1/arg_None_global_model_weights_trainable_None_2Placeholder*
dtype0*
shape:	
d
7update_1/arg_None_global_model_weights_trainable_None_3Placeholder*
dtype0*
shape:
F
update_1/session_token_tensorPlaceholder*
dtype0*
shape: 
N
%update_1/subcomputation/arg_loss_NonePlaceholder*
dtype0*
shape: 
P
'update_1/subcomputation/arg_loss_None_1Placeholder*
dtype0*
shape: 
U
,update_1/subcomputation/session_token_tensorPlaceholder*
dtype0*
shape: 

/update_1/subcomputation/PartitionedCall/truedivRealDivreport/Identityreport/Identity_1*
T02$
truediv__inference_finalize_1507
6
update_1/Identity_10IdentityIdentity_30*
T0
5
update_1/Identity_9IdentityIdentity_29*
T0
6
update_1/Identity_11IdentityIdentity_31*
T0
6
update_1/Identity_12IdentityIdentity_32*
T0

5
update_1/Identity_8IdentityIdentity_28*
T0
5
update_1/Identity_6IdentityIdentity_26*
T0
5
update_1/Identity_7IdentityIdentity_27*
T0
6
update_1/Identity_13IdentityIdentity_33*
T0
5
update_1/Identity_2IdentityIdentity_22*
T0
5
update_1/Identity_3IdentityIdentity_23*
T0
5
update_1/Identity_4IdentityIdentity_24*
T0
5
update_1/Identity_5IdentityIdentity_25*
T0
Z
update_1/Identity_14Identity/update_1/subcomputation/PartitionedCall/truediv*
T0
А
AssignVariableOp_12AssignVariableOp'server/global_model_weights/trainable/0update_1/Identity_2*
dtype0*
validate_shape(*&
 _has_manual_control_dependencies(
o
ReadVariableOp_12ReadVariableOp'server/global_model_weights/trainable/0^AssignVariableOp_12*
dtype0
А
AssignVariableOp_13AssignVariableOp'server/global_model_weights/trainable/1update_1/Identity_3*
dtype0*
validate_shape(*&
 _has_manual_control_dependencies(
o
ReadVariableOp_13ReadVariableOp'server/global_model_weights/trainable/1^AssignVariableOp_13*
dtype0
А
AssignVariableOp_14AssignVariableOp'server/global_model_weights/trainable/2update_1/Identity_4*
dtype0*
validate_shape(*&
 _has_manual_control_dependencies(
o
ReadVariableOp_14ReadVariableOp'server/global_model_weights/trainable/2^AssignVariableOp_14*
dtype0
А
AssignVariableOp_15AssignVariableOp'server/global_model_weights/trainable/3update_1/Identity_5*
dtype0*
validate_shape(*&
 _has_manual_control_dependencies(
o
ReadVariableOp_15ReadVariableOp'server/global_model_weights/trainable/3^AssignVariableOp_15*
dtype0
У
AssignVariableOp_16AssignVariableOp:server/aggregator/query_state/numerator_state/l2_norm_clipupdate_1/Identity_6*
dtype0*
validate_shape(*&
 _has_manual_control_dependencies(

ReadVariableOp_16ReadVariableOp:server/aggregator/query_state/numerator_state/l2_norm_clip^AssignVariableOp_16*
dtype0
Н
AssignVariableOp_17AssignVariableOp4server/aggregator/query_state/numerator_state/stddevupdate_1/Identity_7*
dtype0*
validate_shape(*&
 _has_manual_control_dependencies(
|
ReadVariableOp_17ReadVariableOp4server/aggregator/query_state/numerator_state/stddev^AssignVariableOp_17*
dtype0
В
AssignVariableOp_18AssignVariableOp)server/aggregator/query_state/denominatorupdate_1/Identity_8*
dtype0*
validate_shape(*&
 _has_manual_control_dependencies(
q
ReadVariableOp_18ReadVariableOp)server/aggregator/query_state/denominator^AssignVariableOp_18*
dtype0
Џ
AssignVariableOp_19AssignVariableOp&server/aggregator/dp_event/module_nameupdate_1/Identity_9*
dtype0*
validate_shape(*&
 _has_manual_control_dependencies(
n
ReadVariableOp_19ReadVariableOp&server/aggregator/dp_event/module_name^AssignVariableOp_19*
dtype0
Џ
AssignVariableOp_20AssignVariableOp%server/aggregator/dp_event/class_nameupdate_1/Identity_10*
dtype0*
validate_shape(*&
 _has_manual_control_dependencies(
m
ReadVariableOp_20ReadVariableOp%server/aggregator/dp_event/class_name^AssignVariableOp_20*
dtype0
Е
AssignVariableOp_21AssignVariableOp+server/aggregator/dp_event/noise_multiplierupdate_1/Identity_11*
dtype0*
validate_shape(*&
 _has_manual_control_dependencies(
s
ReadVariableOp_21ReadVariableOp+server/aggregator/dp_event/noise_multiplier^AssignVariableOp_21*
dtype0
Љ
AssignVariableOp_22AssignVariableOpserver/aggregator/is_init_stateupdate_1/Identity_12*
dtype0
*
validate_shape(*&
 _has_manual_control_dependencies(
g
ReadVariableOp_22ReadVariableOpserver/aggregator/is_init_state^AssignVariableOp_22*
dtype0

Ј
AssignVariableOp_23AssignVariableOpserver/finalizer/learning_rateupdate_1/Identity_13*
dtype0*
validate_shape(*&
 _has_manual_control_dependencies(
f
ReadVariableOp_23ReadVariableOpserver/finalizer/learning_rate^AssignVariableOp_23*
dtype0

AssignVariableOp_24AssignVariableOpmetrics/eval/lossupdate_1/Identity_14*
dtype0*
validate_shape(*&
 _has_manual_control_dependencies(
Y
ReadVariableOp_24ReadVariableOpmetrics/eval/loss^AssignVariableOp_24*
dtype0
В
group_deps_6NoOp^AssignVariableOp_12^AssignVariableOp_13^AssignVariableOp_14^AssignVariableOp_15^AssignVariableOp_16^AssignVariableOp_17^AssignVariableOp_18^AssignVariableOp_19^AssignVariableOp_20^AssignVariableOp_21^AssignVariableOp_22^AssignVariableOp_23^AssignVariableOp_24
H
Read_34/ReadVariableOpReadVariableOpmetrics/eval/loss*
dtype0
8
Identity_34IdentityRead_34/ReadVariableOp*
T0"ЇHbР   TFL3                     D     Ј  =               @аџџ         min_runtime_version    P  H  @  8    є  м  Ш  А        X  0    Ш  Ј             x  p  h  `  X  P  H  @  8  0  (           ј  ш  а  Ќ  Є          |  t  l  d  \  H  @  8  0  (           є  р  и  а  И         l  X  D  0  (              ј  №  ш  р  и  а  Ш  Р  И  А  Ј             x  p  h  `  X  P  H  @  8  0  (              ј   №   ш   р   и   а   Ш   Р   И   А   Ј                   x   p   h   T   L   D   <   4   ,   $      Vяџџ      1.15.0          `УџџdУџџhУџџlУџџpУџџtУџџяџџ         УџџУџџУџџУџџУџџУџџ УџџЄУџџЈУџџЌУџџАУџџДУџџИУџџМУџџРУџџФУџџШУџџЬУџџаУџџдУџџиУџџмУџџрУџџфУџџшУџџьУџџ№УџџєУџџјУџџќУџџ ФџџФџџФџџФџџФџџФџџФџџФџџ Фџџ$Фџџ(Фџџ,Фџџ0Фџџ4Фџџ8Фџџ<Фџџ@ФџџDФџџHФџџLФџџPФџџTФџџXФџџ\Фџџ`ФџџdФџџhФџџ~№џџ       ?№џџ      ўџ?№џџ      Пж3Ў№џџ        ?ЌФџџТ№џџ              ж№џџ             ъ№џџ            ьФџџ№Фџџёџџ         ёџџ      џџџџХџџХџџХџџ Хџџ$Хџџ(Хџџ,Хџџ0ХџџFёџџ          DХџџHХџџLХџџPХџџTХџџXХџџ\Хџџ`ХџџdХџџhХџџ~ёџџ                  xy  ёџџ             Вёџџ       Оёџџ       ИХџџМХџџРХџџФХџџШХџџЬХџџаХџџдХџџиХџџмХџџрХџџфХџџшХџџьХџџ№ХџџєХџџјХџџќХџџ ЦџџЦџџђџџ                  6ђџџ   .            .   update/0/loss/0update/0/loss/1  rђџџ               client/0/0/3ђџџ               client/0/0/2Кђџџ               client/0/0/1ођџџ         мЦџџђђџџ          ѓџџ      џџџџџџџџѓџџ          &ѓџџ             :ѓџџ               Rѓџџ               client/0/0/0dЧџџhЧџџlЧџџpЧџџ   MLIR Converted.    <(  и$  А"          ъзџџ       и  ќ        .44_body         Ф   d      Nзџџ      \   $      @зџџ                  	                               lвџџ   D   H      2   OptionalHasValue OptionalHasValue 2  *(              Швџџ   Ј   Ќ         IteratorGetNextAsOptional sIteratorGetNextAsOptional *:
output_shapes)
':џџџџџџџџџ:џџџџџџџџџ*
output_types
2	2  v(            Циџџ               pЩџџ   
         	   	         
                     	                                  $  є  Р    X  ,     д  Ј  p  <  №   Ќ   t   <      Эџџ               4Ъџџ   cond;.82_if2        ЖЭџџ               hЪџџ   cond;.82_if1        RЬџџ                   Ъџџ   cond;.82_if     Ьџџ               $   дЪџџ   OptionalHasValue;.5f        ЦЬџџ               ,   Ыџџ   IteratorGetNextAsOptional;.5b       Эџџ                  \Ыџџ   add;.5e     >Эџџ                  Ыџџ	   add/y;.5a       бџџ            ИЫџџ   arg8        Њбџџ            рЫџџ   arg7        вбџџ      ~      Ьџџ   arg6        њбџџ      }      0Ьџџ   arg5        Юџџ         |         `Ьџџ   arg4        BЮџџ         {         Ьџџ   arg3        rЮџџ         z         РЬџџ   arg2        :аџџ         y      ьЬџџ   arg1        fаџџ         x      Эџџ   arg0        rнџџ           $   H      .44_cond              	                               	     P    ш   Д      \   0      гџџ      w      ФЭџџ   arg8        Жгџџ      v      ьЭџџ   arg7        огџџ      u      Юџџ   arg6        дџџ      t      <Юџџ   arg5        аџџ         s         lЮџџ   arg4        Nаџџ         r         Юџџ   arg3        ~аџџ         q         ЬЮџџ   arg2        Fвџџ         p      јЮџџ   arg1        rвџџ         o      $Яџџ   arg0        ~пџџ   8   Д  Р  р      _functionalize_if_then_branch_00    $   Ш  Є  l  8    и    t  H    ь  Д    \     Ш     x  D     ь  Ш    D     ь  Ф     l  8    а       l   8      zпџџ               $аџџ   :         9   Њпџџ               Tаџџ   9      8   /   кпџџ               аџџ   8      7      
рџџ      *         Даџџ   7      6   6рџџ               раџџ   6      5   4   fрџџ               бџџ   5      2   *   рџџ               @бџџ   4      ,   3   Црџџ               pбџџ   3         2   nћџџ            2      1   ћџџ            1            :сџџ               фбџџ   0         /   тћџџ            /      .   сџџ$       $   (                            .      -      
   
   жсџџ      7         :§џџ      -      &   ~ќџџ            ,      +   &тџџ               авџџ   +         (   Юќџџ            *      )   vтџџ                гџџ   )      (      §џџ            (      '      B§џџ            '      &      ютџџ$       ,   0                                &      %            Bуџџ      	                    ?   %      $   ђ§џџ            $      #   уџџ               Dдџџ   #      "      Ъуџџ               tдџџ   "         !   џџџџvўџџ            !             "фџџ               Ьдџџ                Ъўџџ                           ђўџџ                     фџџ                                          жфџџ               еџџ               џџџџџџџ                         .хџџ               иеџџ               жџџџ                             
     
                         tрџџ          
      OptionalGetValue jOptionalGetValue *:
output_shapes)
':џџџџџџџџџ:џџџџџџџџџ*
output_types
2	2  ~m(                    	   0   :                               ;   ь  Р    h  4    д     0     Д  l     д    0  ш     \    Д  T  ќ     @  а  `    
  №	  	   	  А  X  ф  p  ќ    4  Ь  l    Ф  l    и    d  0  Д  \    Ќ  X     Ќ   d   4      лџџ         n      4иџџ   add;.a2     Ўлџџ         m      `иџџ   mul;.a1     клџџ         l   ,   иџџ   binary_crossentropy/Mean;.9f        jъџџ            k   0      џџџџмиџџ   binary_crossentropy/Neg;.9e       Къџџ            j   4      џџџџ,йџџ   binary_crossentropy/add_2;.9c         ыџџ            i   0      џџџџйџџ   binary_crossentropy/mul;.99       ^ыџџ            h   4      џџџџайџџ   binary_crossentropy/mul_1;.9a         Выџџ            g   4      џџџџ$кџџ   binary_crossentropy/sub_1;.89         ьџџ            f   4      џџџџxкџџ   binary_crossentropy/Cast;.87          Fыџџ            e      T      џџџџакџџ=   binary_crossentropy/remove_squeezable_dimensions/Squeeze;.841         оџџ         d      8лџџ	   add_1;.98       Жоџџ         c      hлџџ   Cast;.95        Nнџџ         b          лџџ   strided_slice;.92       нџџ         a         илџџ	   Shape;.90         Іэџџ            `   4      џџџџмџџ   binary_crossentropy/Log_1;.97         њэџџ            _   4      џџџџlмџџ   binary_crossentropy/add_1;.94         Nюџџ            ^   0      џџџџРмџџ   binary_crossentropy/Log;.96       юџџ            ]   0      џџџџнџџ   binary_crossentropy/add;.93       ююџџ            \   <      џџџџ`нџџ%   binary_crossentropy/clip_by_value;.91         Jяџџ            [   D      џџџџМнџџ-   binary_crossentropy/clip_by_value/Minimum;.8f         Ўяџџ            Z   <      џџџџ оџџ%   example_keras_model/strided_slice;.8e         
№џџ            Y   @      џџџџџџџџоџџ'   example_keras_model/softmax/Softmax;.8d          n№џџ            X   L      џџџџџџџџфоџџ2   example_keras_model/second_dense_layer/Sigmoid;.8c           о№џџ            W   L      џџџџџџџџTпџџ2   example_keras_model/second_dense_layer/BiasAdd;.8b           Nёџџ            V   L      џџџџџџџџФпџџ2   example_keras_model/second_dense_layer/MatMul;.8a6           ъхџџ      U   @    рџџ2   example_keras_model/second_dense_layer/MatMul;.8a5      ў№џџ            T      H      џџџџрџџ2   example_keras_model/second_dense_layer/MatMul;.8a4        jёџџ            S      H      џџџџєрџџ2   example_keras_model/second_dense_layer/MatMul;.8a3        уџџ         R      @   Tсџџ2   example_keras_model/second_dense_layer/MatMul;.8a2      Fѓџџ            Q   |      џџџџџџџџМсџџ`   example_keras_model/first_dense_layer/Relu;.88;example_keras_model/first_dense_layer/BiasAdd;.86             цѓџџ            P   L      џџџџџџџџ\тџџ1   example_keras_model/first_dense_layer/MatMul;.835            шџџ      O   @   Итџџ1   example_keras_model/first_dense_layer/MatMul;.834       ѓџџ            N      H      џџџџ уџџ1   example_keras_model/first_dense_layer/MatMul;.833         єџџ            M      H      џџџџуџџ1   example_keras_model/first_dense_layer/MatMul;.832         хџџ         L      @   ьуџџ1   example_keras_model/first_dense_layer/MatMul;.831       Ъєџџ              K      0      џџџџ   Xфџџ   OptionalGetValue;.811            6іџџ            J   0      џџџџ  Ќфџџ   OptionalGetValue;.81            Іцџџ               @   єфџџ2   example_keras_model/second_dense_layer/MatMul;.8a1      чџџ         	      @   Pхџџ1   example_keras_model/second_dense_layer/MatMul;.8a       ішџџ         G   ,   Јхџџ   binary_crossentropy/sub_2;.76       :щџџ         F   (   ьхџџ   binary_crossentropy/sub;.77     zщџџ         E   ,   ,цџџ   binary_crossentropy/Const_1;.78     Ощџџ         D   ,   pцџџ   binary_crossentropy/sub/x;.79       jшџџ         <      <   Ицџџ.   binary_crossentropy/Mean/reduction_indices;.7a      Тшџџ         B      $   чџџ   strided_slice/stack;.7b       щџџ         A      (   Tчџџ   strided_slice/stack_1;.7c         Nщџџ         @      (   чџџ   strided_slice/stack_2;.7d         щџџ         	      $   фчџџ   strided_slice/stack;.7e       кщџџ               (   (шџџ   strided_slice/stack_1;.7f         "ъџџ         =         pшџџ   .80     Nъџџ         <      L   шџџ<   binary_crossentropy/remove_squeezable_dimensions/Squeeze;.84          Къџџ         ;         щџџ   arg7        ьџџ         :      4щџџ   arg6        Ўьџџ         9      `щџџ   arg5        Bыџџ         8         щџџ   arg4        яџџ      7      Ищџџ   arg3        Њяџџ      6      рщџџ   arg2        вяџџ      5      ъџџ   arg1        њяџџ      4      0ъџџ   arg0        њџџ   8   8   D   d       _functionalize_if_else_branch_00                                                	     T  (  ќ   Ш      h   4      Вьџџ         3          ыџџ   .75     оьџџ         2         ,ыџџ   arg7        Іюџџ         1      Xыџџ   arg6        вюџџ         0      ыџџ   arg5        fэџџ         /         Дыџџ   arg4        Іёџџ      .      мыџџ   arg3        Юёџџ      -      ьџџ   arg2        іёџџ      ,      ,ьџџ   arg1        ђџџ      +      Tьџџ   arg0        Ўќџџ   L   \  d  h  4   __inference_Dataset_map_classfunctools.partial_48410          єіџџ   и   р   	   Ч   ParseExampleV2 ­ParseExampleV2       *
ragged_value_types
 *
dense_shapes
::*
sparse_types
 *

num_sparse *
Tdense
2	*
ragged_split_types
 2  ПА(                                                        H    р   Ќ   h      Ђўџџ              *             џџџџ   0юџџ   .b01                                            )         џџџџ  юџџ   .b0         r№џџ         (         Рюџџ   .ae       Ђ№џџ         '         №юџџ   .ac        в№џџ         &          яџџ   .ab        ђџџ         %      Lяџџ   .aa                                       $            џџџџ яџџ   arg0                            <  <  H     main       Ф  ь    д  d  є      `          Pњџџ   L   L      ;   
SaveSlices %
SaveSlices     *
T
22  3((                                         ]    D                   	                         !   "   	         	   
                  Lћџџ   <   <      ,   MakeIterator MakeIterator  2  $(              ћџџ                AnonymousIteratorV3 kAnonymousIteratorV3*
output_types
2	*:
output_shapes)
':џџџџџџџџџ:џџџџџџџџџ2  n(            Pќџџ   L   P      9   	RestoreV2 $	RestoreV2   *
dtypes
22  1'(                     Мќџџ   L   P      9   	RestoreV2 $	RestoreV2   *
dtypes
22  1'(                     (§џџ   L   P      9   	RestoreV2 $	RestoreV2   *
dtypes
22  1'(                     §џџ   L   P      9   	RestoreV2 $	RestoreV2   *
dtypes
22  1'(                      ўџџ       Є         TakeDataset wTakeDataset  *:
output_shapes)
':џџџџџџџџџ:џџџџџџџџџ*
metadata *
output_types
2	2  z(                 Мўџџ   ,  0       
MapDataset 
MapDataset *:
output_shapes)
':џџџџџџџџџ:џџџџџџџџџ*
use_inter_op_parallelism(*
output_types
2	*=
f8R6
4__inference_Dataset_map_classfunctools.partial_48410*
metadata *

Targuments
 *
preserve_cardinality(2   )                             Є   Ј         BatchDatasetV2 xBatchDatasetV2   *"
output_shapes
:џџџџџџџџџ*
metadata *
parallel_copy( *
output_types
22  {(                                     @   D   2   ExternalDataset ExternalDataset  2  *(                                   #      Ш    T  $  є  Ф    h    д     l  8    а    l  <    м  Д  |  T  ,  Ќ    P    ш   Д      \   0      §џџ      #      Шїџџ   .448        К§џџ      "      №їџџ   .447        т§џџ      !      јџџ   .446        
ўџџ             @јџџ   .445        "њџџ                  pјџџ   .444        Rњџџ                   јџџ   .443        њџџ                  ајџџ   .442        Jќџџ               ќјџџ   .441        vќџџ               (љџџ   .44     ћџџ               `   TљџџS   AnonymousIteratorV3;work/subcomputation/StatefulPartitionedCall/AnonymousIteratorV3     џџџ            Шљџџ   .c      Жџџџ            ьљџџ   .9      кџџџ            њџџ   .6                               Dњџџ   .3      "ќџџ                  pњџџ   .18     Nќџџ                  њџџ   .16     zќџџ                  Шњџџ   .15     Іќџџ                  єњџџ   .12     вќџџ                   ћџџ   .k1       §џџ                  Pћџџ   .a7       2§џџ                  ћџџ   .a6       b§џџ                  Аћџџ   .a        §џџ                  рћџџ   .7        Т§џџ                  ќџџ   .4        ђ§џџ                  @ќџџ	   Const;.21       &ўџџ         	      $   tќџџ   while/loop_counter;.20                                   	      Шќџџ   .1f     Іўџџ                  єќџџ   .17     вўџџ                   §џџ   .14     ўўџџ                  L§џџ   .13     *џџџ                  x§џџ   .11     Vџџџ                  Є§џџ   .1        џџџ                   д§џџ   output_filepath:0       Тџџџ                   ўџџ   input_filepath:0                                             dўџџ   data_token:0            D    ф  М    d  8  (  є  Ш      x  h  X  H  @  ,      ќ   ь   м   Ь   М   Ќ         |   D         Hўџџv      vT§џџ              FlexOptionalHasValue    §џџ              FlexIteratorGetNextAsOptional   Дўџџ(      (Рўџџ;      ;Ьўџџ      иўџџ      фўџџ5      5№ўџџM      MќўџџI      Iџџџ7      7џџџ9      9 џџџ-      -,џџџ      8џџџ           Lџџџ	      	Xџџџ'      'dџџџ)      )pџџџ`      `|џџџn      nўџџ              FlexOptionalGetValue    Дўџџ              FlexParseExampleV2  мўџџ              FlexSaveSlices             w      wџџџ              FlexMakeIterator    @џџџ              FlexAnonymousIteratorV3 lџџџ              FlexRestoreV2   џџџ              FlexTakeDataset Дџџџ              FlexMapDataset  иџџџ              FlexBatchDatasetV2                          FlexExternalDataset 
/*
 * (C) Copyright 2012, IBM Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
define(["dojo/_base/array"], function(arrays) {
	
	function decodeModuleIdList(encoded, moduleIdMap) {
		var idModuleMap = {}, result = [];
		for (var s in moduleIdMap) {
			if (moduleIdMap.hasOwnProperty(s)) {
				idModuleMap[moduleIdMap[s]] = s;
			}
		}
		for (var i = 0, position = -1, length = 0; i < encoded.length;) {
			if (position == -1) {
				// read the position and length values
				position = encoded[i++];
				length = encoded[i++];
			}
			for (var j = 0; j < length; j++) {
				var pluginName = null, moduleName = null, id = encoded[i++];
				if (id === 0) {
					// 0 means the next two ints specify plugin and modulename
					id = encoded[i++];
					pluginName = idModuleMap[id];
					expect(pluginName).not.toBe(null);
					id = encoded[i++];
					moduleName = id !== 0 ? idModuleMap : "";
				} else {
					moduleName = idModuleMap[id];
				}
				expect(moduleName).not.toBe(null);
				expect(result[position+j]).not.toBeDefined();
				result[position+j] = (pluginName !== null ? (pluginName + "!") : "") + moduleName;
			}
			position = -1;
		}
		return result;	
	}
	
	describe("Test addFoldedModuleName", function() {
		it("tests basic path folding", function() {
			var names = ["foo/bar", "foo/baz/yyy", "foo/baz/xxx", "dir"];
			var oFolded = {};
			arrays.forEach(names, function(name, i) {
				addFoldedModuleName({name:name}, i, oFolded, {});
			});
			expect(oFolded).toEqual({foo:{bar:0,baz:{yyy:1,xxx:2}},dir:3});
		});
		
		it("tests path folding with plugin prefixes", function() {
			var oFolded = {}, oPrefixes = {};
			var names = [{name:"foo/bar"},  {name:"foo/baz/yyy.txt",prefix:"combo/text"}, {name:"foo/baz/xxx.txt",prefix:"abc"}];
			arrays.forEach(names, function(name, i) {
				addFoldedModuleName(name, i, oFolded, oPrefixes);
			});
			var expected = {foo:{bar:0,baz:{'yyy.txt':[1, 0],'xxx.txt':[2, 1]}}};
			expected[pluginPrefixesPropName] = {'combo/text':0, abc:1};
			expect(oFolded).toEqual(expected);
		});
		/*
		it("should throw invlalid module name", function() {
			var invalidChars = ['{', '}', ',', ':', '|', '<', '>', '*'];
			var oFolded = {};
			for (var i = 0; i < invalidChars.length; i++) {
				var name = {name:"foo/baz/yyy"+invalidChars[i]};
				var error = null;
				try {
					addFoldedModuleName(name, 0, oFolded, {});
				} catch (e) {
					error = e;
				}
				expect(error).not.toBe(null);
				expect(error.message).toEqual("Invalid module name: " + badName);
			}
			
		});
		*/
	});
	describe("test encodeModules", function() {
		it("tests basic encoding and sorting", function() {
			var result = encodeModules({foo:{bar:0,baz:{yyy:1,xxx:2}},dir:3});
			expect(result).toEqual("(dir!3*foo!(bar!0*baz!(xxx!2*yyy!1)))");
		});
		
		it("tests encoding with module prefixes", function() {
			var obj = {foo:{bar:0,baz:{'yyy.txt':[1, 0],'xxx.txt':[2, 1]}}};
			obj[pluginPrefixesPropName] = {'combo/text':0, abc:1};
			var result = encodeModules(obj);
			expect(result).toEqual("("+pluginPrefixesPropName+"!(abc!1*combo/text!0)*foo!(bar!0*baz!(xxx.txt!2-1*yyy.txt!1-0)))");
		});
		
		it("tests special character handling", function() {
			var result = encodeModules({foo:{'(bar)':0,baz:{yyy:1,xxx:2}},'dir!':3});
			expect(result).toEqual("(dir|!3*foo!(<bar>!0*baz!(xxx!2*yyy!1)))");
		});
	});
	
	describe("test addModuleIdEncoded", function() {
		var moduleIdMap = {"foo/bar1":11, "foo/bar2":12, "dir1/dir2/test":25, "xxx/yyy": 50};
		it("test basic encoding and positioning", function() {
			var names = ["foo/bar1","xxx/yyy",null,null,"dir1/dir2/test","foo/bar2","notfound"];
			var encoded = [];
			arrays.forEach(names, function(name, i) {
				if (name) {
					var added = addModuleIdEncoded({name:name}, i, encoded, moduleIdMap);
					var expected = (name != "notfound");
					expect(added).toBe(expected);
				}
			});
			var moduleIdDecoded = decodeModuleIdList(encoded, moduleIdMap);
			expect(moduleIdDecoded).toEqual(["foo/bar1","xxx/yyy",undefined,undefined,"dir1/dir2/test","foo/bar2"]);
			
		});
	});
	
	describe("test moduleIdsFromHasLoaderExpression", function() {
		it("dojo/has!feature?module1:module2", function() {
			var mids = [];
			moduleIdsFromHasLoaderExpression("dojo/has!feature?module1:module2", mids);
			expect(mids).toEqual(["module1", "module2"]);
		});
		it("dojo/has!feature?module1:module2", function() {
			var mids = [];
			moduleIdsFromHasLoaderExpression("dojo/has!feature?module1", mids);
			expect(mids).toEqual(["module1"]);
		});
		it("dojo/has!feature?:module2", function() {
			var mids = [];
			moduleIdsFromHasLoaderExpression("dojo/has!feature?:module2", mids);
			expect(mids).toEqual(["module2"]);
		});
		it("dojo/has!feature?featureA?moduleA1:moduleA2", function() {
			var mids = [];
			moduleIdsFromHasLoaderExpression("dojo/has!feature?featureA?moduleA1:moduleA2", mids);
			expect(mids).toEqual(["moduleA1", "moduleA2"]);
		});
		it("dojo/has!feature?featureA?:moduleA1:moduleA2", function() {
			var mids = [];
			moduleIdsFromHasLoaderExpression("dojo/has!feature?:featureA?moduleA1:moduleA2", mids);
			expect(mids).toEqual(["moduleA1", "moduleA2"]);
		});
		it("dojo/has!feature?featureA?moduleA1:moduleA2:module2", function() {
			var mids = [];
			moduleIdsFromHasLoaderExpression("dojo/has!feature?featureA?moduleA1:moduleA2:module2", mids);
			expect(mids).toEqual(["moduleA1", "moduleA2", "module2"]);
		});
		it("dojo/has!feature?featureA?moduleA1:moduleA2:featureB?moduleB1:moduleB2", function() {
			var mids = [];
			moduleIdsFromHasLoaderExpression("dojo/has!feature?featureA?moduleA1:moduleA2:featureB?moduleB1:moduleB2", mids);
			expect(mids).toEqual(["moduleA1", "moduleA2", "moduleB1", "moduleB2"]);
		});
	});
	
	describe("test registerModuleNameIds", function() {
		it("simple registration", function() {
			var moduleIdMap = {};
			registerModuleNameIds([[["dep1", "dep2"]],[[101, 102]]], moduleIdMap);
			expect(moduleIdMap.dep1).toBe(101);
			expect(moduleIdMap.dep2).toBe(102);
		});
		it("multi scope registration", function() {
			var moduleIdMap = {};
			registerModuleNameIds([[["dep1","dep2"],["plugin!dep3"],["dep4"]],[[101,102],[103],[104]]], moduleIdMap);
			expect(moduleIdMap.dep1).toBe(101);
			expect(moduleIdMap.dep2).toBe(102);
			expect(moduleIdMap.dep3).toBe(103);
			expect(moduleIdMap.dep4).toBe(104);
		});
		it("has loader plugin with single module", function() {
			var moduleIdMap = {};
			registerModuleNameIds([[["dep5", "has!feature?plugin!fdep1", "dep6"]],[[105,201,106]]], moduleIdMap);
			expect(moduleIdMap.dep5).toBe(105);
			expect(moduleIdMap.dep6).toBe(106);
			expect(moduleIdMap.fdep1).toBe(201);
		});
		it("has loader plugin with two modules", function() {
			var moduleIdMap = {};
			registerModuleNameIds([[["dep5", "has!feature?fdep1:plugin!fdep2", "dep6"]],[[105,[201, 202],106]]], moduleIdMap);
			expect(moduleIdMap.dep5).toBe(105);
			expect(moduleIdMap.dep6).toBe(106);
			expect(moduleIdMap.fdep1).toBe(201);
			expect(moduleIdMap.fdep2).toBe(202);
		});
		it("has loader plugin with three modules", function() {
			var moduleIdMap = {};
			registerModuleNameIds([[["dep1", "has!feature1?fdep1:feture2?fdep2:fdep3", "dep2"]],[[101,[201, 202, 203],102]]], moduleIdMap);
			expect(moduleIdMap.dep1).toBe(101);
			expect(moduleIdMap.dep2).toBe(102);
			expect(moduleIdMap.fdep1).toBe(201);
			expect(moduleIdMap.fdep2).toBe(202);
			expect(moduleIdMap.fdep3).toBe(203);
		});
		it("attempted re-assignment", function() {
			var moduleIdMap = {}, error;
			registerModuleNameIds([[["dep1", "dep2"]],[[101, 102]]], moduleIdMap);
			try {
				registerModuleNameIds([[["dep2"]],[[202]]]);
			} catch (e) {
				error = e;
			}
			expect(error).toBeDefined();
		});
	});
});
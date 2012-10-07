Ember.ENV.RAISE_ON_DEPRECATION = true;

var EurekaJ = Ember.Application.create({
    log: function(message) {
        if (window.console) console.log(message);
    }
});

//Removing the Camelcase-to-dash convention from Ember Data
DS.Model.reopen({
    namingConvention: {
        keyToJSONKey: function(key) {
            return key;
        },

        foreignKey: function(key) {
            return key;
        }
    }
});

DS.Model.reopen({
    reload: function() {
        if (!this.get('isDirty') && this.get('isLoaded')) {
            var store = this.get('store'),
                adapter = store.get('adapter');

            adapter.find(store, this.constructor, this.get('id'));
        }
    }
})

EurekaJ.Adapter = DS.Adapter.create({
    findAll: function(store, type) {
        var url = type.url;

        EurekaJ.log('finding all: type: ' + type + ' url: ' + url);

        $.ajax({
        	  type: 'GET',
        	  url: url,
        	  contentType: 'application/json',
        	  success: function(data) { EurekaJ.store.loadMany(type, data); }
        	});
    },
    
    find: function(store, type, id) {
    	var url = type.url;

        var requestStringJson = {};
        requestStringJson.id = id;

        EurekaJ.log('finding: type: ' + type + ' id: ' + id + ' url: ' + url);

        $.ajax({
      	  type: 'GET',
      	  url: url,
      	  data: JSON.stringify(requestStringJson, null, '\t'),
      	  contentType: 'application/json',
      	  success: function(data) { EurekaJ.store.load(type, data); }
      	});
    },

    findQuery: function(store, type, query, modelArray) {
        EurekaJ.log('FINDQUERY');
        EurekaJ.log(query);
        EurekaJ.log(modelArray);
    },

    updateRecord: function(store, type, model) {
        var url = type.url;

        EurekaJ.log('updating record: type: ' + type + ' id: ' + model.get('id') + ' url: ' + url);
        EurekaJ.log('json: ' + JSON.stringify(model));

        jQuery.ajax({
            url: url,
            data: JSON.stringify(model),
            dataType: 'json',
            type: 'PUT',

            success: function(data) {
                // data is a hash of key/value pairs representing the record
                // in its current state on the server.
                store.didUpdateRecord(model, data);
            }
        });
    }
});

EurekaJ.ajaxSuccess = function(data) {
	EurekaJ.Store.loadMany(type, data);
}

EurekaJ.store = DS.Store.create({
    adapter: EurekaJ.Adapter,
    //adapter:  DS.RESTAdapter.create({ bulkCommit: false }),
    revision: 4
});